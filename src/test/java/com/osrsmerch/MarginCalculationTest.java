package com.osrsmerch;

import com.osrsmerch.model.ItemPriceData;
import org.junit.Assert;
import org.junit.Test;

public class MarginCalculationTest {

    @Test
    public void testStandardMarginAndTax() {
        // Example: Item selling for High: 1,500,000, Low: 1,400,000
        ItemPriceData data = ItemPriceData.builder()
            .high(1_500_000)
            .highTime(1700000000L)
            .low(1_400_000)
            .lowTime(1700000010L)
            .build();

        double taxRate = 2.0; // 2%
        int taxCap = 5_000_000;

        int gross = data.getGrossMargin();
        Assert.assertEquals(100_000, gross);

        // 2% of 1,500,000 is 30,000
        int tax = data.calculateTax(taxRate, taxCap);
        Assert.assertEquals(30_000, tax);

        // Net margin = 100,000 - 30,000 = 70,000
        int net = data.getNetMargin(taxRate, taxCap);
        Assert.assertEquals(70_000, net);

        // ROI = (70,000 / 1,400,000) * 100 = 5.0%
        double roi = data.getRoi(taxRate, taxCap);
        Assert.assertEquals(5.0, roi, 0.001);

        // Potential Limit Profit for limit of 8 items = 70,000 * 8 = 560,000
        long limitProfit = data.getPotentialLimitProfit(8, taxRate, taxCap);
        Assert.assertEquals(560_000L, limitProfit);
    }

    @Test
    public void testTaxCapOnHighValueItem() {
        // High value item: High = 1,500,000,000 (1.5B), Low = 1,480,000,000 (1.48B)
        // 2% of 1.5B would be 30,000,000, but capped at 5,000,000 GP
        ItemPriceData data = ItemPriceData.builder()
            .high(1_500_000_000)
            .highTime(1700000000L)
            .low(1_480_000_000)
            .lowTime(1700000000L)
            .build();

        double taxRate = 2.0;
        int taxCap = 5_000_000;

        int tax = data.calculateTax(taxRate, taxCap);
        Assert.assertEquals(5_000_000, tax);

        int netMargin = data.getNetMargin(taxRate, taxCap);
        Assert.assertEquals(15_000_000, netMargin); // 20M gross - 5M tax
    }

    @Test
    public void testZeroAndNegativeMargins() {
        ItemPriceData data = ItemPriceData.builder()
            .high(100)
            .low(100)
            .build();

        int tax = data.calculateTax(2.0, 5_000_000);
        Assert.assertEquals(2, tax);

        int netMargin = data.getNetMargin(2.0, 5_000_000);
        Assert.assertEquals(-2, netMargin);
    }

    @Test
    public void testLowPriceItemTaxExemption() {
        // Items under 100 GP have 0% GE tax
        ItemPriceData lowItem = ItemPriceData.builder()
            .high(95)
            .low(90)
            .build();

        Assert.assertTrue(lowItem.isTaxExempt());
        int tax = lowItem.calculateTax(2.0, 5_000_000);
        Assert.assertEquals(0, tax);

        int net = lowItem.getNetMargin(2.0, 5_000_000);
        Assert.assertEquals(5, net);
    }

    @Test
    public void testPriceTrendMomemtum() {
        ItemPriceData priceData = ItemPriceData.builder()
            .high(1_550_000)
            .low(1_450_000)
            .build();

        com.osrsmerch.model.ItemVolumeData volumeDataLowerAvg = com.osrsmerch.model.ItemVolumeData.builder()
            .avgHighPrice(1_500_000)
            .highPriceVolume(100)
            .avgLowPrice(1_400_000)
            .lowPriceVolume(50)
            .build();

        Assert.assertEquals(ItemPriceData.PriceTrend.RISING, priceData.getTrend(volumeDataLowerAvg));

        com.osrsmerch.model.ItemVolumeData volumeDataHigherAvg = com.osrsmerch.model.ItemVolumeData.builder()
            .avgHighPrice(1_600_000)
            .highPriceVolume(100)
            .avgLowPrice(1_400_000)
            .lowPriceVolume(50)
            .build();

        Assert.assertEquals(ItemPriceData.PriceTrend.FALLING, priceData.getTrend(volumeDataHigherAvg));

        com.osrsmerch.model.ItemVolumeData volumeDataEqualAvg = com.osrsmerch.model.ItemVolumeData.builder()
            .avgHighPrice(1_550_000)
            .highPriceVolume(100)
            .avgLowPrice(1_450_000)
            .lowPriceVolume(50)
            .build();

        Assert.assertEquals(ItemPriceData.PriceTrend.STABLE, priceData.getTrend(volumeDataEqualAvg));
        Assert.assertFalse(priceData.isTaxExempt());
    }
}
