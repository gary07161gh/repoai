package com.osrsmerch.ui;

import com.osrsmerch.OsrsMerchConfig;
import com.osrsmerch.model.ItemMapping;
import com.osrsmerch.model.ItemPriceData;
import com.osrsmerch.service.OsrsWikiPriceService;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;

@Singleton
public class HeaderRenderer {

    private final ItemManager itemManager;
    private final OsrsMerchConfig config;
    private final OsrsWikiPriceService priceService;

    @Inject
    public HeaderRenderer(
        ItemManager itemManager,
        OsrsMerchConfig config,
        OsrsWikiPriceService priceService
    ) {
        this.itemManager = itemManager;
        this.config = config;
        this.priceService = priceService;
    }

    public void renderHeader(
        Graphics2D g,
        int itemId,
        ItemMapping mapping,
        ItemPriceData priceData,
        int x,
        int y,
        int w,
        int h,
        long now
    ) {
        // Draw item sprite icon
        BufferedImage icon = itemManager.getImage(itemId);
        if (icon != null) {
            g.drawImage(icon, x, y, 32, 32, null);
        }

        int textX = x + 36;
        String itemName = mapping != null && mapping.getName() != null 
            ? mapping.getName() 
            : "Item #" + itemId;

        g.setFont(OverlayTheme.FONT_HEADER);
        g.setColor(Color.WHITE);
        g.drawString(itemName, textX, y + 16);

        // Item Buy Limit Badge
        int limit = mapping != null ? mapping.getLimit() : 0;
        g.setFont(OverlayTheme.FONT_LABEL);
        String limitStr = limit > 0 ? "4h Limit: " + OverlayTheme.formatGp(limit) : "4h Limit: None";
        g.setColor(OverlayTheme.TEXT_CYAN);
        g.drawString(limitStr, textX, y + 29);

        // Right-aligned Live Status & Freshness & Sync Time
        String statusStr;
        Color statusColor;
        long lastSync = priceService.getLastSuccessfulSyncTime();
        long syncAge = lastSync > 0 ? (System.currentTimeMillis() - lastSync) / 1000L : -1;
        String syncSuffix = syncAge >= 0 ? " | Sync " + (syncAge < 60 ? syncAge + "s" : (syncAge / 60) + "m") + " ago" : "";

        if (priceData != null && (priceData.getHigh() > 0 || priceData.getLow() > 0)) {
            long newestAge = Math.min(
                priceData.getHighTime() > 0 ? now - priceData.getHighTime() : Long.MAX_VALUE,
                priceData.getLowTime() > 0 ? now - priceData.getLowTime() : Long.MAX_VALUE
            );
            statusStr = "● Real-Time (" + (newestAge < 60 ? newestAge + "s" : (newestAge / 60) + "m") + " ago)" + syncSuffix;
            statusColor = config.positiveProfitColor();
        } else {
            statusStr = "● No Live Trades" + syncSuffix;
            statusColor = OverlayTheme.TEXT_MUTED;
        }

        FontMetrics fm = g.getFontMetrics();
        int statusW = fm.stringWidth(statusStr);
        g.setColor(statusColor);
        g.drawString(statusStr, x + w - statusW - 2, y + 16);

        // Tax rate or Exemption tag in header
        String taxTag = (priceData != null && priceData.isTaxExempt()) 
            ? "Tax: 0% (Exempt <100gp)" 
            : "Tax: " + config.taxPercentage() + "%";
        int taxW = fm.stringWidth(taxTag);
        g.setColor(OverlayTheme.TEXT_GOLD);
        g.drawString(taxTag, x + w - taxW - 2, y + 29);
    }
}
