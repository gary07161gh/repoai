package com.osrsmerch.ui;

import org.junit.Assert;
import org.junit.Test;

public class OverlayThemeTest {

    @Test
    public void testFormatGp() {
        Assert.assertEquals("0", OverlayTheme.formatGp(0));
        Assert.assertEquals("500", OverlayTheme.formatGp(500));
        Assert.assertEquals("1,500,000", OverlayTheme.formatGp(1_500_000));
        Assert.assertEquals("-25,000", OverlayTheme.formatGp(-25_000));
    }

    @Test
    public void testFormatShortGp() {
        Assert.assertEquals("0", OverlayTheme.formatShortGp(0));
        Assert.assertEquals("500", OverlayTheme.formatShortGp(500));
        Assert.assertEquals("999", OverlayTheme.formatShortGp(999));
        Assert.assertEquals("1.5K", OverlayTheme.formatShortGp(1_500));
        Assert.assertEquals("10K", OverlayTheme.formatShortGp(10_000));
        Assert.assertEquals("1.25M", OverlayTheme.formatShortGp(1_250_000));
        Assert.assertEquals("50M", OverlayTheme.formatShortGp(50_000_000));
        Assert.assertEquals("1.5B", OverlayTheme.formatShortGp(1_500_000_000L));
        Assert.assertEquals("-1.25M", OverlayTheme.formatShortGp(-1_250_000));
    }

    @Test
    public void testFormatPercent() {
        Assert.assertEquals("+5.25%", OverlayTheme.formatPercent(0.0525));
        Assert.assertEquals("-1.50%", OverlayTheme.formatPercent(-0.0150));
        Assert.assertEquals("+0.00%", OverlayTheme.formatPercent(0.0));
    }
}
