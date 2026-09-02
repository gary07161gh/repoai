package com.osrsmerch.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;

/**
 * Shared design tokens, cached typography/stroke instances, and formatting utilities
 * used across OSRS Merch Overlay UI components.
 */
public final class OverlayTheme {

    private OverlayTheme() {
        // Utility class
    }

    // Colors
    public static final Color BG_TOP = new Color(20, 24, 33, 245);
    public static final Color BG_BOTTOM = new Color(13, 16, 23, 250);
    public static final Color BORDER_OUTER = new Color(45, 52, 68);
    public static final Color CARD_BG = new Color(25, 30, 42, 200);
    public static final Color CARD_BORDER = new Color(50, 58, 76, 180);
    public static final Color BTN_BG = new Color(34, 41, 56);
    public static final Color BTN_HOVER_BG = new Color(48, 58, 80);
    public static final Color BTN_BORDER = new Color(68, 79, 104);
    public static final Color BTN_COPIED_BG = new Color(20, 70, 45);
    public static final Color TEXT_MUTED = new Color(156, 163, 175);
    public static final Color TEXT_CYAN = new Color(56, 189, 248);
    public static final Color TEXT_GOLD = new Color(251, 191, 36);
    public static final Color GRID_COLOR = new Color(40, 46, 62, 120);
    public static final Color AXIS_LABEL_COLOR = new Color(130, 140, 160);
    public static final Color BUY_VOL_COLOR = new Color(56, 189, 248, 180);
    public static final Color SELL_VOL_COLOR = new Color(251, 146, 60, 180);

    // Cached Fonts
    public static final Font FONT_HEADER = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    public static final Font FONT_VALUE = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    public static final Font FONT_LARGE_VALUE = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_TINY = new Font(Font.SANS_SERIF, Font.PLAIN, 9);
    public static final Font FONT_CARD_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 9);
    public static final Font FONT_BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    public static final Font FONT_ITALIC = new Font(Font.SANS_SERIF, Font.ITALIC, 11);
    public static final Font FONT_AXIS = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    public static final Font FONT_LEGEND = new Font(Font.SANS_SERIF, Font.BOLD, 8);

    // Cached Strokes
    public static final BasicStroke BORDER_STROKE = new BasicStroke(1.2f);
    public static final BasicStroke PRICE_LINE_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static final BasicStroke GRID_STROKE = new BasicStroke(0.5f);

    // Formats
    private static final DecimalFormat GP_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat SHORT_GP_FORMAT = new DecimalFormat("0.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("+0.00%;-0.00%");

    /**
     * Formats integer/long amounts with commas (e.g. 1,500,000).
     */
    public static synchronized String formatGp(long amount) {
        return GP_FORMAT.format(amount);
    }

    /**
     * Formats compact GP notations (e.g. 1.5M, 500K, 2.1B).
     */
    public static synchronized String formatShortGp(long amount) {
        if (amount == 0) {
            return "0";
        }
        if (Math.abs(amount) >= 1_000_000_000) {
            return SHORT_GP_FORMAT.format(amount / 1_000_000_000.0) + "B";
        }
        if (Math.abs(amount) >= 1_000_000) {
            return SHORT_GP_FORMAT.format(amount / 1_000_000.0) + "M";
        }
        if (Math.abs(amount) >= 1_000) {
            return SHORT_GP_FORMAT.format(amount / 1_000.0) + "K";
        }
        return GP_FORMAT.format(amount);
    }

    /**
     * Formats percentage ratio with explicit +/- sign (e.g. +5.20% or -1.50%).
     */
    public static synchronized String formatPercent(double ratio) {
        return PERCENT_FORMAT.format(ratio);
    }
}
