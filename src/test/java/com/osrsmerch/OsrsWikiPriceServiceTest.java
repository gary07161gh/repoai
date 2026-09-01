package com.osrsmerch;

import com.google.gson.Gson;
import com.osrsmerch.service.OsrsWikiPriceService;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class OsrsWikiPriceServiceTest {

    @Test
    public void testPriceServiceParsing() {
        OkHttpClient httpClient = new OkHttpClient();
        Gson gson = new Gson();

        // Sample config with 2% tax
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
    }
}
