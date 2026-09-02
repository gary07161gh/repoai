package com.osrsmerch.ui;

import com.osrsmerch.GeInputHandler;
import com.osrsmerch.OsrsMerchConfig;
import com.osrsmerch.model.ItemPriceData;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QuickButtonsRenderer {

    private final OsrsMerchConfig config;
    private final GeInputHandler inputHandler;

    @Inject
    public QuickButtonsRenderer(OsrsMerchConfig config, GeInputHandler inputHandler) {
        this.config = config;
        this.inputHandler = inputHandler;
    }

    public void renderQuickActionButtons(
        Graphics2D g,
        ItemPriceData priceData,
        int x,
        int y,
        int w,
        int h
    ) {
        int buttonCount = 4;
        int gap = 5;
        int btnW = (w - (gap * (buttonCount - 1))) / buttonCount;

        int buyOffset = config.buyOffset();
        int sellOffset = config.sellOffset();

        int instaSellPrice = priceData != null ? priceData.getLow() : 0;
        int outbidPrice = instaSellPrice > 0 ? instaSellPrice + buyOffset : 0;

        int instaBuyPrice = priceData != null ? priceData.getHigh() : 0;
        int undercutPrice = instaBuyPrice > 0 ? Math.max(1, instaBuyPrice - sellOffset) : 0;

        // Button 1: Set Insta-Sell
        drawButton(
            g,
            GeInputHandler.ButtonType.SET_SELL_INSTA,
            "⚡ Bid (" + OverlayTheme.formatShortGp(instaSellPrice) + ")",
            instaSellPrice,
            x,
            y,
            btnW,
            h
        );

        // Button 2: Outbid (+offset)
        int b2X = x + btnW + gap;
        drawButton(
            g,
            GeInputHandler.ButtonType.SET_BUY_OUTBID,
            "⚡ Outbid +" + buyOffset + " (" + OverlayTheme.formatShortGp(outbidPrice) + ")",
            outbidPrice,
            b2X,
            y,
            btnW,
            h
        );

        // Button 3: Undercut (-offset)
        int b3X = b2X + btnW + gap;
        drawButton(
            g,
            GeInputHandler.ButtonType.SET_SELL_UNDERCUT,
            "⚡ Undercut -" + sellOffset + " (" + OverlayTheme.formatShortGp(undercutPrice) + ")",
            undercutPrice,
            b3X,
            y,
            btnW,
            h
        );

        // Button 4: Set Insta-Buy
        int b4X = b3X + btnW + gap;
        drawButton(
            g,
            GeInputHandler.ButtonType.SET_BUY_INSTA,
            "⚡ Ask (" + OverlayTheme.formatShortGp(instaBuyPrice) + ")",
            instaBuyPrice,
            b4X,
            y,
            btnW,
            h
        );
    }

    private void drawButton(
        Graphics2D g,
        GeInputHandler.ButtonType type,
        String label,
        int price,
        int x,
        int y,
        int w,
        int h
    ) {
        Rectangle bounds = new Rectangle(x, y, w, h);
        inputHandler.registerButton(type, bounds, price);

        boolean isHovered = inputHandler.getHoveredButton() == type;
        boolean isRecentlyCopied = inputHandler.getLastCopiedPrice() == price 
            && (System.currentTimeMillis() - inputHandler.getLastCopiedTime() < 2000);

        g.setColor(isRecentlyCopied ? OverlayTheme.BTN_COPIED_BG : (isHovered ? OverlayTheme.BTN_HOVER_BG : OverlayTheme.BTN_BG));
        g.fillRoundRect(x, y, w, h, 4, 4);

        g.setColor(isRecentlyCopied ? config.positiveProfitColor() : (isHovered ? config.accentColor() : OverlayTheme.BTN_BORDER));
        g.drawRoundRect(x, y, w, h, 4, 4);

        g.setFont(OverlayTheme.FONT_BUTTON);
        String displayLabel = isRecentlyCopied ? "✓ Copied!" : label;
        g.setColor(isRecentlyCopied ? config.positiveProfitColor() : (price > 0 ? (isHovered ? Color.WHITE : OverlayTheme.TEXT_GOLD) : OverlayTheme.TEXT_MUTED));

        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(displayLabel);
        int textX = x + Math.max(2, (w - textW) / 2);
        int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();

        g.drawString(displayLabel, textX, textY);
    }
}
