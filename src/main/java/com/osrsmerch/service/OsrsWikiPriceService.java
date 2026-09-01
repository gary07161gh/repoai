package com.osrsmerch.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.osrsmerch.OsrsMerchConfig;
import com.osrsmerch.model.ItemMapping;
import com.osrsmerch.model.ItemPriceData;
import com.osrsmerch.model.ItemVolumeData;
import com.osrsmerch.model.TimeseriesDataPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class OsrsWikiPriceService {
    private static final String API_BASE = "https://prices.runescape.wiki/api/v1/osrs";
    private static final String LATEST_PRICES_URL = API_BASE + "/latest";
    private static final String VOLUME_5M_URL = API_BASE + "/5m";
    private static final String MAPPING_URL = API_BASE + "/mapping";
    private static final String TIMESERIES_URL = API_BASE + "/timeseries";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final OsrsMerchConfig config;

    private final Map<Integer, ItemPriceData> priceCache = new ConcurrentHashMap<>();
    private final Map<Integer, ItemVolumeData> volumeCache = new ConcurrentHashMap<>();
    private final Map<Integer, ItemMapping> mappingCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<TimeseriesDataPoint>> timeseriesCache = new ConcurrentHashMap<>();
    private volatile int currentTimeseriesItemId = -1;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OSRS-Merch-PriceFetcher");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask;
    private volatile boolean isRunning = false;
    private volatile long lastSuccessfulSyncTime = 0L;

    @Inject
    public OsrsWikiPriceService(OkHttpClient httpClient, Gson gson, OsrsMerchConfig config) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.config = config;
    }

    public synchronized void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        log.info("Starting OSRS Wiki Real-time Price Service...");

        // Initial mapping load
        fetchItemMapping();

        // Initial price & volume load
        fetchAllPrices();
        fetch5mVolumes();

        // Schedule periodic sync
        scheduleSyncTask();
    }

    public synchronized void restartScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        if (isRunning) {
            scheduleSyncTask();
        }
    }

    private void scheduleSyncTask() {
        int interval = Math.max(5, config.refreshIntervalSeconds());
        scheduledTask = executor.scheduleAtFixedRate(() -> {
            try {
                fetchAllPrices();
                fetch5mVolumes();
            } catch (Exception e) {
                log.warn("Error during periodic OSRS Wiki price sync: {}", e.getMessage());
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        isRunning = false;
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            scheduledTask = null;
        }
        log.info("Stopped OSRS Wiki Price Service.");
    }

    public synchronized void shutdown() {
        stop();
        executor.shutdownNow();
    }

    public ItemPriceData getPrice(int itemId) {
        return priceCache.get(itemId);
    }

    public ItemVolumeData getVolume(int itemId) {
        return volumeCache.get(itemId);
    }

    public ItemMapping getMapping(int itemId) {
        return mappingCache.get(itemId);
    }

    public List<TimeseriesDataPoint> getTimeseries(int itemId) {
        return timeseriesCache.getOrDefault(itemId, Collections.emptyList());
    }

    /**
     * Fetches 24h of 5-minute timeseries data for the given item.
     * Only fetches if the item has changed since the last request.
     */
    public void fetchTimeseriesIfNeeded(int itemId) {
        if (itemId <= 0 || itemId == currentTimeseriesItemId) {
            return;
        }
        currentTimeseriesItemId = itemId;
        fetchTimeseries(itemId);
    }

    public long getLastSuccessfulSyncTime() {
        return lastSuccessfulSyncTime;
    }

    public boolean isLoaded() {
        return !priceCache.isEmpty() && !mappingCache.isEmpty();
    }

    private String getEffectiveUserAgent() {
        String userAgent = config.customUserAgent();
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "RuneLite-OsrsMerchOverlay";
        }
        return userAgent.trim();
    }

    public void fetchAllPrices() {
        Request request = new Request.Builder()
            .url(LATEST_PRICES_URL)
            .header("User-Agent", getEffectiveUserAgent())
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("Failed to fetch OSRS Wiki latest prices: {}", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        log.warn("Unsuccessful response from OSRS Wiki latest prices: HTTP {}", response.code());
                        return;
                    }

                    String json = response.body().string();
                    JsonObject root = gson.fromJson(json, JsonObject.class);
                    if (root != null && root.has("data")) {
                        JsonObject data = root.getAsJsonObject("data");
                        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                            try {
                                int itemId = Integer.parseInt(entry.getKey());
                                JsonObject priceObj = entry.getValue().getAsJsonObject();

                                int high = priceObj.has("high") && !priceObj.get("high").isJsonNull() 
                                    ? priceObj.get("high").getAsInt() : 0;
                                long highTime = priceObj.has("highTime") && !priceObj.get("highTime").isJsonNull() 
                                    ? priceObj.get("highTime").getAsLong() : 0L;
                                int low = priceObj.has("low") && !priceObj.get("low").isJsonNull() 
                                    ? priceObj.get("low").getAsInt() : 0;
                                long lowTime = priceObj.has("lowTime") && !priceObj.get("lowTime").isJsonNull() 
                                    ? priceObj.get("lowTime").getAsLong() : 0L;

                                priceCache.put(itemId, new ItemPriceData(high, highTime, low, lowTime));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        lastSuccessfulSyncTime = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    log.error("Failed to parse OSRS Wiki latest prices json: {}", e.getMessage(), e);
                }
            }
        });
    }

    public void fetch5mVolumes() {
        Request request = new Request.Builder()
            .url(VOLUME_5M_URL)
            .header("User-Agent", getEffectiveUserAgent())
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("Failed to fetch OSRS Wiki 5m volumes: {}", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }

                    String json = response.body().string();
                    JsonObject root = gson.fromJson(json, JsonObject.class);
                    if (root != null && root.has("data")) {
                        JsonObject data = root.getAsJsonObject("data");
                        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                            try {
                                int itemId = Integer.parseInt(entry.getKey());
                                JsonObject volObj = entry.getValue().getAsJsonObject();

                                int avgHigh = volObj.has("avgHighPrice") && !volObj.get("avgHighPrice").isJsonNull()
                                    ? volObj.get("avgHighPrice").getAsInt() : 0;
                                int highVol = volObj.has("highPriceVolume") && !volObj.get("highPriceVolume").isJsonNull()
                                    ? volObj.get("highPriceVolume").getAsInt() : 0;
                                int avgLow = volObj.has("avgLowPrice") && !volObj.get("avgLowPrice").isJsonNull()
                                    ? volObj.get("avgLowPrice").getAsInt() : 0;
                                int lowVol = volObj.has("lowPriceVolume") && !volObj.get("lowPriceVolume").isJsonNull()
                                    ? volObj.get("lowPriceVolume").getAsInt() : 0;

                                volumeCache.put(itemId, new ItemVolumeData(avgHigh, highVol, avgLow, lowVol));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse OSRS Wiki 5m volumes: {}", e.getMessage());
                }
            }
        });
    }

    public void fetchItemMapping() {
        Request request = new Request.Builder()
            .url(MAPPING_URL)
            .header("User-Agent", getEffectiveUserAgent())
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("Failed to fetch OSRS Wiki item mapping: {}", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }

                    String json = response.body().string();
                    Type listType = new TypeToken<List<ItemMapping>>() {}.getType();
                    List<ItemMapping> items = gson.fromJson(json, listType);
                    if (items != null) {
                        for (ItemMapping item : items) {
                            mappingCache.put(item.getId(), item);
                        }
                        log.info("Successfully cached {} OSRS Wiki item mappings.", items.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse OSRS Wiki item mapping: {}", e.getMessage());
                }
            }
        });
    }

    private void fetchTimeseries(int itemId) {
        String url = TIMESERIES_URL + "?timestep=5m&id=" + itemId;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", getEffectiveUserAgent())
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("Failed to fetch timeseries for item {}: {}", itemId, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        log.warn("Unsuccessful timeseries response for item {}: HTTP {}", itemId, response.code());
                        return;
                    }

                    String json = response.body().string();
                    JsonObject root = gson.fromJson(json, JsonObject.class);
                    if (root != null && root.has("data")) {
                        JsonArray dataArr = root.getAsJsonArray("data");
                        List<TimeseriesDataPoint> points = new ArrayList<>();
                        for (JsonElement elem : dataArr) {
                            JsonObject obj = elem.getAsJsonObject();
                            long ts = obj.has("timestamp") && !obj.get("timestamp").isJsonNull()
                                ? obj.get("timestamp").getAsLong() : 0L;
                            int avgHigh = obj.has("avgHighPrice") && !obj.get("avgHighPrice").isJsonNull()
                                ? obj.get("avgHighPrice").getAsInt() : 0;
                            int avgLow = obj.has("avgLowPrice") && !obj.get("avgLowPrice").isJsonNull()
                                ? obj.get("avgLowPrice").getAsInt() : 0;
                            int highVol = obj.has("highPriceVolume") && !obj.get("highPriceVolume").isJsonNull()
                                ? obj.get("highPriceVolume").getAsInt() : 0;
                            int lowVol = obj.has("lowPriceVolume") && !obj.get("lowPriceVolume").isJsonNull()
                                ? obj.get("lowPriceVolume").getAsInt() : 0;

                            if (ts > 0) {
                                points.add(new TimeseriesDataPoint(ts, avgHigh, avgLow, highVol, lowVol));
                            }
                        }
                        // Sort by timestamp ascending
                        points.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                        timeseriesCache.put(itemId, points);
                        log.info("Loaded {} timeseries points for item {}", points.size(), itemId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse timeseries for item {}: {}", itemId, e.getMessage());
                }
            }
        });
    }
}
