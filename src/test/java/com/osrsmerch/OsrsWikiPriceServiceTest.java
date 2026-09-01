package com.osrsmerch;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.osrsmerch.model.ItemMapping;
import com.osrsmerch.model.ItemPriceData;
import com.osrsmerch.model.ItemVolumeData;
import com.osrsmerch.service.OsrsWikiPriceService;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class OsrsWikiPriceServiceTest {

    @Test
    public void testPriceServiceInitialization() {
        OkHttpClient httpClient = new OkHttpClient();
        Gson gson = new Gson();

        OsrsMerchConfig config = new OsrsMerchConfig() {
            @Override
            public double taxPercentage() {
                return 2.0;
            }

            @Override
            public int taxCap() {
                return 5000000;
            }

            @Override
            public int refreshIntervalSeconds() {
                return 10;
            }

            @Override
            public String customUserAgent() {
                return "RuneLite-OsrsMerchOverlay-Test";
            }
        };

        OsrsWikiPriceService service = new OsrsWikiPriceService(httpClient, gson, config);
        Assert.assertNotNull(service);
        Assert.assertFalse(service.isLoaded());
    }

    @Test
    public void testLatestPricesJsonParsing() {
        Gson gson = new Gson();
        String json = "{"
            + "\"data\": {"
            + "  \"4151\": {\"high\": 1500000, \"highTime\": 1700000000, \"low\": 1480000, \"lowTime\": 1700000005},"
            + "  \"11802\": {\"high\": 25000000, \"highTime\": 1700000010, \"low\": 24800000, \"lowTime\": 1700000015}"
            + "}"
            + "}";

        JsonObject root = gson.fromJson(json, JsonObject.class);
        Assert.assertNotNull(root);
        JsonObject data = root.getAsJsonObject("data");
        Map<Integer, ItemPriceData> parsed = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            int itemId = Integer.parseInt(entry.getKey());
            JsonObject obj = entry.getValue().getAsJsonObject();
            int high = obj.get("high").getAsInt();
            long highTime = obj.get("highTime").getAsLong();
            int low = obj.get("low").getAsInt();
            long lowTime = obj.get("lowTime").getAsLong();
            parsed.put(itemId, new ItemPriceData(high, highTime, low, lowTime));
        }

        Assert.assertEquals(2, parsed.size());
        ItemPriceData whip = parsed.get(4151);
        Assert.assertNotNull(whip);
        Assert.assertEquals(1500000, whip.getHigh());
        Assert.assertEquals(1480000, whip.getLow());
        Assert.assertEquals(20000, whip.getGrossMargin());
    }

    @Test
    public void test5mVolumesJsonParsing() {
        Gson gson = new Gson();
        String json = "{"
            + "\"data\": {"
            + "  \"4151\": {\"avgHighPrice\": 1502000, \"highPriceVolume\": 45, \"avgLowPrice\": 1481000, \"lowPriceVolume\": 32}"
            + "}"
            + "}";

        JsonObject root = gson.fromJson(json, JsonObject.class);
        Assert.assertNotNull(root);
        JsonObject data = root.getAsJsonObject("data");
        JsonObject whipObj = data.getAsJsonObject("4151");

        ItemVolumeData volume = new ItemVolumeData(
            whipObj.get("avgHighPrice").getAsInt(),
            whipObj.get("highPriceVolume").getAsInt(),
            whipObj.get("avgLowPrice").getAsInt(),
            whipObj.get("lowPriceVolume").getAsInt()
        );

        Assert.assertEquals(1502000, volume.getAvgHighPrice());
        Assert.assertEquals(45, volume.getHighPriceVolume());
        Assert.assertEquals(1481000, volume.getAvgLowPrice());
        Assert.assertEquals(32, volume.getLowPriceVolume());
        Assert.assertEquals(77, volume.getTotalVolume());
    }

    @Test
    public void testMappingJsonParsing() {
        Gson gson = new Gson();
        String json = "["
            + "{\"id\": 4151, \"name\": \"Abyssal whip\", \"limit\": 70, \"highalch\": 72000, \"value\": 120001, \"members\": true},"
            + "{\"id\": 11802, \"name\": \"Armadyl godsword\", \"limit\": 8, \"highalch\": 750000, \"value\": 1250000, \"members\": true}"
            + "]";

        Type listType = new TypeToken<List<ItemMapping>>() {}.getType();
        List<ItemMapping> items = gson.fromJson(json, listType);

        Assert.assertNotNull(items);
        Assert.assertEquals(2, items.size());
        ItemMapping whip = items.get(0);
        Assert.assertEquals(4151, whip.getId());
        Assert.assertEquals("Abyssal whip", whip.getName());
        Assert.assertEquals(70, whip.getLimit());
        Assert.assertTrue(whip.isMembers());
    }
}
