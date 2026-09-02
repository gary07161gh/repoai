package com.osrsmerch;

import com.osrsmerch.model.TimeseriesDataPoint;
import com.osrsmerch.ui.OverlayTheme;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/**
 * Renders a 24-hour price/volume chart in the style of the OSRS Wiki graph.
 * Price lines (instabuy green, instasell pink) with volume bars underneath.
 */
public class PriceGraphRenderer {

    private final Color instabuyColor;
    private final Color instasellColor;
    private final Color buyVolColor;
    private final Color sellVolColor;

    public PriceGraphRenderer(Color instabuyColor, Color instasellColor) {
        this.instabuyColor = instabuyColor;
        this.instasellColor = instasellColor;
        this.buyVolColor = OverlayTheme.BUY_VOL_COLOR;
        this.sellVolColor = OverlayTheme.SELL_VOL_COLOR;
    }

    /**
     * Renders the complete price/volume graph.
     * @param g       Graphics context
     * @param points  24h timeseries data points (sorted ascending by timestamp)
     * @param x       Left edge of graph area
     * @param y       Top edge of graph area
     * @param w       Width of graph area
     * @param h       Height of graph area
     */
    public void render(Graphics2D g, List<TimeseriesDataPoint> points, int x, int y, int w, int h) {
        if (points == null || points.size() < 2) {
            g.setFont(OverlayTheme.FONT_ITALIC);
            g.setColor(OverlayTheme.AXIS_LABEL_COLOR);
            g.drawString("Loading price history...", x + w / 2 - 55, y + h / 2);
            return;
        }

        Stroke origStroke = g.getStroke();

        int leftMargin = 42;
        int rightMargin = 4;
        int topMargin = 14;
        int bottomMargin = 12;

        int chartX = x + leftMargin;
        int chartW = w - leftMargin - rightMargin;

        // Split: 70% price chart, 5% gap, 25% volume
        int priceH = (int) ((h - topMargin - bottomMargin) * 0.68);
        int volumeH = (int) ((h - topMargin - bottomMargin) * 0.25);
        int priceY = y + topMargin;
        int volumeY = priceY + priceH + 4;

        // Render legend
        renderLegend(g, x, y, w);

        // Calculate price range
        int minPrice = Integer.MAX_VALUE;
        int maxPrice = Integer.MIN_VALUE;
        int maxVolume = 0;
        for (TimeseriesDataPoint p : points) {
            if (p.getAvgHighPrice() > 0) {
                minPrice = Math.min(minPrice, p.getAvgHighPrice());
                maxPrice = Math.max(maxPrice, p.getAvgHighPrice());
            }
            if (p.getAvgLowPrice() > 0) {
                minPrice = Math.min(minPrice, p.getAvgLowPrice());
                maxPrice = Math.max(maxPrice, p.getAvgLowPrice());
            }
            maxVolume = Math.max(maxVolume, Math.max(p.getHighPriceVolume(), p.getLowPriceVolume()));
        }

        if (minPrice >= maxPrice) {
            // Flat price — expand range by 1%
            int mid = minPrice;
            minPrice = (int) (mid * 0.995);
            maxPrice = (int) (mid * 1.005);
            if (minPrice == maxPrice) {
                maxPrice = minPrice + 1;
            }
        }

        // Add 5% padding to price range
        int priceRange = maxPrice - minPrice;
        minPrice -= (int) (priceRange * 0.05);
        maxPrice += (int) (priceRange * 0.05);
        if (minPrice < 0) {
            minPrice = 0;
        }

        if (maxVolume == 0) {
            maxVolume = 1;
        }

        long minTime = points.get(0).getTimestamp();
        long maxTime = points.get(points.size() - 1).getTimestamp();
        long timeRange = maxTime - minTime;
        if (timeRange <= 0) {
            timeRange = 1;
        }

        // Draw grid lines for price area
        g.setStroke(OverlayTheme.GRID_STROKE);
        g.setFont(OverlayTheme.FONT_AXIS);
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            int gy = priceY + (int) ((double) i / gridLines * priceH);
            g.setColor(OverlayTheme.GRID_COLOR);
            g.drawLine(chartX, gy, chartX + chartW, gy);

            // Y-axis price label
            int priceVal = maxPrice - (int) ((double) i / gridLines * (maxPrice - minPrice));
            g.setColor(OverlayTheme.AXIS_LABEL_COLOR);
            String label = OverlayTheme.formatShortGp(priceVal);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, chartX - fm.stringWidth(label) - 3, gy + fm.getAscent() / 2);
        }

        // Draw X-axis time labels
        long nowEpoch = System.currentTimeMillis() / 1000L;
        int xLabels = Math.min(6, chartW / 60);
        for (int i = 0; i <= xLabels; i++) {
            int lx = chartX + (int) ((double) i / xLabels * chartW);
            long t = minTime + (long) ((double) i / xLabels * timeRange);
            long hoursAgo = (nowEpoch - t) / 3600;
            long minsAgo = ((nowEpoch - t) % 3600) / 60;

            String timeLabel;
            if (hoursAgo >= 1) {
                timeLabel = hoursAgo + "h ago";
            } else {
                timeLabel = minsAgo + "m ago";
            }

            g.setColor(OverlayTheme.AXIS_LABEL_COLOR);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(timeLabel, lx - fm.stringWidth(timeLabel) / 2, priceY + priceH + bottomMargin);

            // Subtle vertical grid line
            g.setColor(OverlayTheme.GRID_COLOR);
            g.drawLine(lx, priceY, lx, priceY + priceH);
        }

        // Draw price lines
        g.setStroke(OverlayTheme.PRICE_LINE_STROKE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int prevHighX = -1, prevHighY = -1;
        int prevLowX = -1, prevLowY = -1;

        for (TimeseriesDataPoint p : points) {
            int px = chartX + (int) ((double) (p.getTimestamp() - minTime) / timeRange * chartW);

            // Draw instabuy (high) line
            if (p.getAvgHighPrice() > 0) {
                int py = priceY + priceH - (int) ((double) (p.getAvgHighPrice() - minPrice) / (maxPrice - minPrice) * priceH);
                py = Math.max(priceY, Math.min(priceY + priceH, py));
                if (prevHighX >= 0) {
                    g.setColor(instabuyColor);
                    g.drawLine(prevHighX, prevHighY, px, py);
                }
                prevHighX = px;
                prevHighY = py;
            }

            // Draw instasell (low) line
            if (p.getAvgLowPrice() > 0) {
                int py = priceY + priceH - (int) ((double) (p.getAvgLowPrice() - minPrice) / (maxPrice - minPrice) * priceH);
                py = Math.max(priceY, Math.min(priceY + priceH, py));
                if (prevLowX >= 0) {
                    g.setColor(instasellColor);
                    g.drawLine(prevLowX, prevLowY, px, py);
                }
                prevLowX = px;
                prevLowY = py;
            }
        }

        // Draw volume bars
        g.setStroke(origStroke);
        int barWidth = Math.max(1, chartW / points.size());

        for (TimeseriesDataPoint p : points) {
            int px = chartX + (int) ((double) (p.getTimestamp() - minTime) / timeRange * chartW);

            // Buy volume bar (upward from baseline)
            if (p.getHighPriceVolume() > 0) {
                int barH = Math.max(1, (int) ((double) p.getHighPriceVolume() / maxVolume * volumeH));
                g.setColor(buyVolColor);
                g.fillRect(px, volumeY + volumeH - barH, Math.max(1, barWidth - 1), barH);
            }

            // Sell volume bar (separate color, stacked or adjacent)
            if (p.getLowPriceVolume() > 0) {
                int barH = Math.max(1, (int) ((double) p.getLowPriceVolume() / maxVolume * volumeH));
                g.setColor(sellVolColor);
                g.fillRect(px + barWidth, volumeY + volumeH - barH, Math.max(1, barWidth - 1), barH);
            }
        }

        g.setStroke(origStroke);
    }

    private void renderLegend(Graphics2D g, int x, int y, int w) {
        g.setFont(OverlayTheme.FONT_LEGEND);
        FontMetrics fm = g.getFontMetrics();
        int dotSize = 6;
        int gap = 8;
        int lx = x + 44;
        int ly = y + 4;

        // Instabuy price
        g.setColor(instabuyColor);
        g.fillOval(lx, ly, dotSize, dotSize);
        lx += dotSize + 3;
        g.drawString("Instabuy price", lx, ly + fm.getAscent() - 1);
        lx += fm.stringWidth("Instabuy price") + gap;

        // Instasell price
        g.setColor(instasellColor);
        g.fillOval(lx, ly, dotSize, dotSize);
        lx += dotSize + 3;
        g.drawString("Instasell price", lx, ly + fm.getAscent() - 1);
        lx += fm.stringWidth("Instasell price") + gap;

        // Buy volume
        g.setColor(buyVolColor);
        g.fillRect(lx, ly, dotSize, dotSize);
        lx += dotSize + 3;
        g.drawString("Buy vol", lx, ly + fm.getAscent() - 1);
        lx += fm.stringWidth("Buy vol") + gap;

        // Sell volume
        g.setColor(sellVolColor);
        g.fillRect(lx, ly, dotSize, dotSize);
        lx += dotSize + 3;
        g.drawString("Sell vol", lx, ly + fm.getAscent() - 1);
    }
}
