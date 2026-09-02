package com.osrsmerch.ui;

import com.osrsmerch.OsrsMerchConfig;
import com.osrsmerch.model.ItemMapping;
import com.osrsmerch.model.ItemPriceData;
import com.osrsmerch.model.ItemVolumeData;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MetricsGridRenderer {

    private final OsrsMerchConfig config;

    @Inject
    public MetricsGridRenderer(OsrsMerchConfig config) {
        this.config = config;
    }

    /**
     * Compact horizontal summary strip (Ask, Bid, Net Margin, ROI, 5m Volume)
     * displayed when the price chart is visible.
     */
    public void renderSummaryStrip(
        Graphics2D g,
        ItemPriceData priceData,
        ItemMapping mapping,
        ItemVolumeData volumeData,
        int x,
        int y,
        int w,
        int h,
        long now
    ) {
        g.setColor(OverlayTheme.CARD_BG);
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(OverlayTheme.CARD_BORDER);
        g.drawRoundRect(x, y, w, h, 4, 4);

        if (priceData == null) {
            g.setFont(OverlayTheme.FONT_ITALIC);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("Fetching price metrics...", x + 8, y + 13);
            return;
        }

        double taxRate = config.taxPercentage();
        int taxCap = config.taxCap();
        int net = priceData.getNetMargin(taxRate, taxCap);
        double roi = priceData.getRoi(taxRate, taxCap);

        int curX = x + 8;
        int textY = y + 13;

        // Ask
        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("Ask: ", curX, textY);
        curX += g.getFontMetrics().stringWidth("Ask: ");
        g.setFont(OverlayTheme.FONT_VALUE);
        g.setColor(Color.WHITE);
        String askStr = priceData.getHigh() > 0 ? OverlayTheme.formatGp(priceData.getHigh()) + " gp" : "None";
        g.drawString(askStr, curX, textY);
        curX += g.getFontMetrics().stringWidth(askStr) + 12;

        // Bid
        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("Bid: ", curX, textY);
        curX += g.getFontMetrics().stringWidth("Bid: ");
        g.setFont(OverlayTheme.FONT_VALUE);
        g.setColor(Color.WHITE);
        String bidStr = priceData.getLow() > 0 ? OverlayTheme.formatGp(priceData.getLow()) + " gp" : "None";
        g.drawString(bidStr, curX, textY);
        curX += g.getFontMetrics().stringWidth(bidStr) + 12;

        // Net Margin
        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("Margin: ", curX, textY);
        curX += g.getFontMetrics().stringWidth("Margin: ");
        g.setFont(OverlayTheme.FONT_VALUE);
        Color marginColor = net > 0 ? config.positiveProfitColor() : (net < 0 ? config.negativeProfitColor() : OverlayTheme.TEXT_MUTED);
        g.setColor(marginColor);
        String marginStr = (net > 0 ? "+" : "") + OverlayTheme.formatGp(net) + " gp";
        if (config.showRoi() && priceData.getLow() > 0) {
            marginStr += " (" + OverlayTheme.formatPercent(roi / 100.0) + " ROI)";
        }
        g.drawString(marginStr, curX, textY);
        curX += g.getFontMetrics().stringWidth(marginStr) + 12;

        // 5m Volume
        if (config.showVolumeMetrics() && volumeData != null) {
            g.setFont(OverlayTheme.FONT_LABEL);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("5m Vol: ", curX, textY);
            curX += g.getFontMetrics().stringWidth("5m Vol: ");
            g.setFont(OverlayTheme.FONT_VALUE);
            g.setColor(OverlayTheme.TEXT_CYAN);
            String volStr = OverlayTheme.formatGp(volumeData.getTotalVolume()) + " (▲" + volumeData.getHighPriceVolume() + " ▼" + volumeData.getLowPriceVolume() + ")";
            g.drawString(volStr, curX, textY);
        }
    }

    /**
     * 3-column metric cards grid: Prices, Margins & Momentum, 5M Trade Velocity.
     */
    public void renderMetricsGrid(
        Graphics2D g,
        ItemPriceData priceData,
        ItemMapping mapping,
        ItemVolumeData volumeData,
        int x,
        int y,
        int w,
        int h,
        long now
    ) {
        int colGap = 6;
        int colWidth = (w - (colGap * 2)) / 3;

        // Card 1: Prices
        renderCard(g, x, y, colWidth, h, "MARKET PRICES");
        renderPriceColumn(g, priceData, volumeData, x + 6, y + 18, colWidth - 12, now);

        // Card 2: Margins & Profit
        int col2X = x + colWidth + colGap;
        renderCard(g, col2X, y, colWidth, h, "MARGIN & MOMENTUM");
        renderMarginColumn(g, priceData, mapping, volumeData, col2X + 6, y + 18, colWidth - 12);

        // Card 3: Liquidity & 5m Volume
        int col3X = col2X + colWidth + colGap;
        renderCard(g, col3X, y, colWidth, h, "5M TRADE VELOCITY");
        renderVolumeColumn(g, volumeData, col3X + 6, y + 18, colWidth - 12);
    }

    private void renderCard(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(OverlayTheme.CARD_BG);
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(OverlayTheme.CARD_BORDER);
        g.drawRoundRect(x, y, w, h, 4, 4);

        g.setFont(OverlayTheme.FONT_CARD_TITLE);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString(title, x + 6, y + 12);
    }

    private void renderPriceColumn(
        Graphics2D g,
        ItemPriceData priceData,
        ItemVolumeData volumeData,
        int x,
        int y,
        int w,
        long now
    ) {
        if (priceData == null) {
            g.setFont(OverlayTheme.FONT_ITALIC);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("Fetching prices...", x, y + 18);
            return;
        }

        // Insta-Buy (High / Ask)
        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("Insta-Buy (Ask):", x, y + 12);

        g.setFont(OverlayTheme.FONT_VALUE);
        g.setColor(Color.WHITE);
        String highStr = priceData.getHigh() > 0 ? OverlayTheme.formatGp(priceData.getHigh()) + " gp" : "None";
        g.drawString(highStr, x, y + 25);

        g.setFont(OverlayTheme.FONT_TINY);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString(priceData.getHighAgeFormatted(now), x, y + 36);

        // Insta-Sell (Low / Bid)
        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("Insta-Sell (Bid):", x, y + 49);

        g.setFont(OverlayTheme.FONT_VALUE);
        g.setColor(Color.WHITE);
        String lowStr = priceData.getLow() > 0 ? OverlayTheme.formatGp(priceData.getLow()) + " gp" : "None";
        g.drawString(lowStr, x, y + 62);

        g.setFont(OverlayTheme.FONT_TINY);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString(priceData.getLowAgeFormatted(now), x, y + 73);
    }

    private void renderMarginColumn(
        Graphics2D g,
        ItemPriceData priceData,
        ItemMapping mapping,
        ItemVolumeData volumeData,
        int x,
        int y,
        int w
    ) {
        if (priceData == null || priceData.getHigh() <= 0 || priceData.getLow() <= 0) {
            g.setFont(OverlayTheme.FONT_ITALIC);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("No margin data", x, y + 18);
            return;
        }

        double taxRate = config.taxPercentage();
        int taxCap = config.taxCap();

        int gross = priceData.getGrossMargin();
        int tax = priceData.calculateTax(taxRate, taxCap);
        int net = priceData.getNetMargin(taxRate, taxCap);
        double roi = priceData.getRoi(taxRate, taxCap);

        // Net Margin
        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("Net Margin:", x, y + 12);

        g.setFont(OverlayTheme.FONT_LARGE_VALUE);
        Color marginColor = net > 0 ? config.positiveProfitColor() : config.negativeProfitColor();
        g.setColor(marginColor);
        String netStr = (net > 0 ? "+" : "") + OverlayTheme.formatGp(net) + " gp";
        g.drawString(netStr, x, y + 25);

        // Tax deduction line / Exemption
        g.setFont(OverlayTheme.FONT_TINY);
        g.setColor(OverlayTheme.TEXT_GOLD);
        String taxStr = priceData.isTaxExempt() 
            ? "Tax: 0 gp (Exempt)" 
            : "Tax: -" + OverlayTheme.formatGp(tax) + " gp (Gross: " + OverlayTheme.formatGp(gross) + ")";
        g.drawString(taxStr, x, y + 36);

        // ROI %
        if (config.showRoi()) {
            g.setFont(OverlayTheme.FONT_LABEL);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("ROI:", x, y + 49);

            g.setFont(OverlayTheme.FONT_VALUE);
            g.setColor(marginColor);
            g.drawString(OverlayTheme.formatPercent(roi / 100.0), x + 26, y + 49);
        }

        // Limit Potential Profit & Short-term momentum trend
        if (config.showBuyLimitProfit() && mapping != null && mapping.getLimit() > 0) {
            long limitProfit = priceData.getPotentialLimitProfit(mapping.getLimit(), taxRate, taxCap);
            g.setFont(OverlayTheme.FONT_LABEL);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("Limit Profit: ", x, y + 62);

            g.setFont(OverlayTheme.FONT_VALUE);
            g.setColor(marginColor);
            g.drawString(OverlayTheme.formatShortGp(limitProfit), x, y + 74);
        } else {
            // Display price momentum trend badge
            ItemPriceData.PriceTrend trend = priceData.getTrend(volumeData);
            g.setFont(OverlayTheme.FONT_LABEL);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("Momentum: ", x, y + 62);

            g.setFont(OverlayTheme.FONT_CARD_TITLE);
            g.setColor(trend.getColor());
            g.drawString(trend.getLabel(), x, y + 74);
        }
    }

    private void renderVolumeColumn(
        Graphics2D g,
        ItemVolumeData volumeData,
        int x,
        int y,
        int w
    ) {
        if (!config.showVolumeMetrics()) {
            g.setFont(OverlayTheme.FONT_LABEL);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("Volumes hidden in config", x, y + 20);
            return;
        }

        if (volumeData == null) {
            g.setFont(OverlayTheme.FONT_ITALIC);
            g.setColor(OverlayTheme.TEXT_MUTED);
            g.drawString("No 5m trades recorded", x, y + 20);
            return;
        }

        int bought = volumeData.getHighPriceVolume();
        int sold = volumeData.getLowPriceVolume();
        int total = volumeData.getTotalVolume();

        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(OverlayTheme.TEXT_MUTED);
        g.drawString("5m Trades: ", x, y + 12);

        g.setFont(OverlayTheme.FONT_VALUE);
        g.setColor(Color.WHITE);
        g.drawString(OverlayTheme.formatGp(total) + " items", x + 60, y + 12);

        g.setFont(OverlayTheme.FONT_LABEL);
        g.setColor(config.positiveProfitColor());
        g.drawString("▲ Bought: " + OverlayTheme.formatGp(bought), x, y + 28);

        g.setColor(OverlayTheme.TEXT_GOLD);
        g.drawString("▼ Sold: " + OverlayTheme.formatGp(sold), x, y + 42);

        // Liquidity Tag
        String liquidityRating;
        Color liqColor;
        if (total > 50) {
            liquidityRating = "HIGH LIQUIDITY";
            liqColor = config.positiveProfitColor();
        } else if (total > 10) {
            liquidityRating = "MODERATE LIQUIDITY";
            liqColor = OverlayTheme.TEXT_CYAN;
        } else if (total > 0) {
            liquidityRating = "LOW LIQUIDITY";
            liqColor = OverlayTheme.TEXT_GOLD;
        } else {
            liquidityRating = "ILLIQUID / SLOW";
            liqColor = config.negativeProfitColor();
        }

        g.setFont(OverlayTheme.FONT_CARD_TITLE);
        g.setColor(liqColor);
        g.drawString("● " + liquidityRating, x, y + 66);
    }
}
