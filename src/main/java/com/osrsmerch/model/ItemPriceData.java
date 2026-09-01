package com.osrsmerch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPriceData {
    private int high;
    private long highTime;
    private int low;
    private long lowTime;

    /**
     * Calculates gross margin (High - Low)
     */
    public int getGrossMargin() {
        if (high <= 0 || low <= 0) {
            return 0;
        }
        return high - low;
    }

    /**
     * Calculates Grand Exchange Tax per item.
     * Standard OSRS GE tax is 2% of the selling (high) price, capped at 5,000,000 GP.
     * Items sold for under 100 GP are exempt from tax if standard rules apply,
     * but configurable tax percentage and cap are supported here.
     */
    public int calculateTax(double taxRatePercent, int taxCap) {
        if (high <= 0 || taxRatePercent <= 0) {
            return 0;
        }
        // In OSRS, items sold for under 100 GP are exempt from GE tax
        if (high < 100) {
            return 0;
        }
        // Tax applies to the final sale price (High price)
        long rawTax = (long) Math.floor((high * taxRatePercent) / 100.0);
        return (int) Math.min(taxCap, rawTax);
    }

    /**
     * Calculates net margin after GE tax.
     */
    public int getNetMargin(double taxRatePercent, int taxCap) {
        if (high <= 0 || low <= 0) {
            return 0;
        }
        int tax = calculateTax(taxRatePercent, taxCap);
        return (high - low) - tax;
    }

    /**
     * Calculates Return on Investment (ROI) percentage based on low buy price.
     */
    public double getRoi(double taxRatePercent, int taxCap) {
        if (low <= 0 || high <= 0) {
            return 0.0;
        }
        int netMargin = getNetMargin(taxRatePercent, taxCap);
        return ((double) netMargin / (double) low) * 100.0;
    }

    /**
     * Calculates total potential profit for a full 4-hour buy limit quantity.
     */
    public long getPotentialLimitProfit(int buyLimit, double taxRatePercent, int taxCap) {
        if (buyLimit <= 0) {
            return 0L;
        }
        int netMargin = getNetMargin(taxRatePercent, taxCap);
        return (long) netMargin * buyLimit;
    }

    /**
     * Returns human-readable relative age of high price (e.g., "4s ago", "2m ago")
     */
    public String getHighAgeFormatted(long currentEpochSeconds) {
        return formatAge(currentEpochSeconds - highTime);
    }

    /**
     * Returns human-readable relative age of low price
     */
    public String getLowAgeFormatted(long currentEpochSeconds) {
        return formatAge(currentEpochSeconds - lowTime);
    }

    public enum PriceTrend {
        RISING("▲ RISING", new java.awt.Color(0, 230, 118)),
        FALLING("▼ FALLING", new java.awt.Color(255, 82, 82)),
        STABLE("▬ STABLE", new java.awt.Color(56, 189, 248)),
        UNKNOWN("● UNKNOWN", new java.awt.Color(156, 163, 175));

        private final String label;
        private final java.awt.Color color;

        PriceTrend(String label, java.awt.Color color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public java.awt.Color getColor() {
            return color;
        }
    }

    /**
     * Determines short-term price momentum by comparing instant ask price with 5-minute average.
     */
    public PriceTrend getTrend(ItemVolumeData volumeData) {
        if (volumeData == null || volumeData.getAvgHighPrice() <= 0 || high <= 0) {
            return PriceTrend.UNKNOWN;
        }
        int diff = high - volumeData.getAvgHighPrice();
        if (diff > 0) {
            return PriceTrend.RISING;
        } else if (diff < 0) {
            return PriceTrend.FALLING;
        }
        return PriceTrend.STABLE;
    }

    /**
     * Checks if item qualifies for full GE tax exemption (< 100 GP)
     */
    public boolean isTaxExempt() {
        return high > 0 && high < 100;
    }

    private String formatAge(long diffSeconds) {
        if (diffSeconds < 0) {
            return "just now";
        }
        if (diffSeconds < 60) {
            return diffSeconds + "s ago";
        }
        long minutes = diffSeconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        return hours + "h ago";
    }
}
