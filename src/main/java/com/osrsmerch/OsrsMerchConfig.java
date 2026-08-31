package com.osrsmerch;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("osrsmerch")
public interface OsrsMerchConfig extends Config {

    @ConfigSection(
        name = "Tax & Calculations",
        description = "Grand Exchange tax and margin calculation settings",
        position = 0
    )
    String taxSection = "taxSection";

    @ConfigItem(
        keyName = "taxPercentage",
        name = "GE Tax Rate (%)",
        description = "The Grand Exchange tax percentage applied to sales (Default is 2.0%)",
        section = taxSection,
        position = 1
    )
    default double taxPercentage() {
        return 2.0;
    }

    @ConfigItem(
        keyName = "taxCap",
        name = "GE Tax Cap (GP)",
        description = "The maximum GE tax charged on any transaction (Default is 5,000,000 GP)",
        section = taxSection,
        position = 2
    )
    default int taxCap() {
        return 5000000;
    }

    @ConfigSection(
        name = "Quick Actions & Offsets",
        description = "Quick price buttons and offset adjustments",
        position = 10
    )
    String actionSection = "actionSection";

    @ConfigItem(
        keyName = "enableQuickButtons",
        name = "Enable Quick Action Buttons",
        description = "Displays clickable quick action buttons over the chatbox overlay",
        section = actionSection,
        position = 11
    )
    default boolean enableQuickButtons() {
        return true;
    }

    @ConfigItem(
        keyName = "buyOffset",
        name = "Outbid Offset (+GP)",
        description = "GP added to Instant-Sell price to outbid competitor buy offers",
        section = actionSection,
        position = 12
    )
    default int buyOffset() {
        return 1;
    }

    @ConfigItem(
        keyName = "sellOffset",
        name = "Undercut Offset (-GP)",
        description = "GP subtracted from Instant-Buy price to undercut competitor sell offers",
        section = actionSection,
        position = 13
    )
    default int sellOffset() {
        return 1;
    }

    @ConfigSection(
        name = "Display & Metrics",
        description = "Toggle visible metrics on the chatbox overlay",
        position = 20
    )
    String displaySection = "displaySection";

    @ConfigItem(
        keyName = "showVolumeMetrics",
        name = "Show Trade Volumes",
        description = "Show 5-minute trade volume and buy/sell liquidity",
        section = displaySection,
        position = 21
    )
    default boolean showVolumeMetrics() {
        return true;
    }

    @ConfigItem(
        keyName = "showRoi",
        name = "Show ROI %",
        description = "Display Return On Investment percentage",
        section = displaySection,
        position = 22
    )
    default boolean showRoi() {
        return true;
    }

    @ConfigItem(
        keyName = "showBuyLimitProfit",
        name = "Show Limit Potential Profit",
        description = "Display theoretical profit for 4-hour buy limit",
        section = displaySection,
        position = 23
    )
    default boolean showBuyLimitProfit() {
        return true;
    }

    @ConfigSection(
        name = "API & Sync",
        description = "OSRS Wiki real-time API sync configuration",
        position = 30
    )
    String apiSection = "apiSection";

    @Range(min = 5, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(
        keyName = "refreshIntervalSeconds",
        name = "Refresh Interval",
        description = "How often real-time prices sync in background (seconds)",
        section = apiSection,
        position = 31
    )
    default int refreshIntervalSeconds() {
        return 10;
    }

    @ConfigItem(
        keyName = "customUserAgent",
        name = "Custom User-Agent",
        description = "User-Agent header sent to the OSRS Wiki API (Wiki requirement)",
        section = apiSection,
        position = 32
    )
    default String customUserAgent() {
        return "RuneLite-OsrsMerchOverlay";
    }

    @ConfigSection(
        name = "Theme & Colors",
        description = "Visual color styling for margins and highlights",
        position = 40
    )
    String themeSection = "themeSection";

    @ConfigItem(
        keyName = "positiveProfitColor",
        name = "Profit Color",
        description = "Color used for positive profit and margins",
        section = themeSection,
        position = 41
    )
    default Color positiveProfitColor() {
        return new Color(0, 230, 118);
    }

    @ConfigItem(
        keyName = "negativeProfitColor",
        name = "Loss Color",
        description = "Color used for negative profit / loss",
        section = themeSection,
        position = 42
    )
    default Color negativeProfitColor() {
        return new Color(255, 82, 82);
    }

    @ConfigItem(
        keyName = "accentColor",
        name = "Accent Color",
        description = "Header and border accent color",
        section = themeSection,
        position = 43
    )
    default Color accentColor() {
        return new Color(255, 183, 77);
    }
}
