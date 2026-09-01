package com.osrsmerch;

import com.osrsmerch.model.ItemMapping;
import com.osrsmerch.model.ItemPriceData;
import com.osrsmerch.model.ItemVolumeData;
import com.osrsmerch.service.OsrsWikiPriceService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
@Singleton
public class OsrsMerchOverlay extends Overlay {

    private static final DecimalFormat GP_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat SHORT_GP_FORMAT = new DecimalFormat("0.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("+0.00%;-0.00%");

    // Color Palette
    private static final Color BG_TOP = new Color(20, 24, 33, 245);
    private static final Color BG_BOTTOM = new Color(13, 16, 23, 250);
    private static final Color BORDER_OUTER = new Color(45, 52, 68);
    private static final Color CARD_BG = new Color(25, 30, 42, 200);
    private static final Color CARD_BORDER = new Color(50, 58, 76, 180);
    private static final Color BTN_BG = new Color(34, 41, 56);
    private static final Color BTN_HOVER_BG = new Color(48, 58, 80);
    private static final Color BTN_BORDER = new Color(68, 79, 104);
    private static final Color TEXT_MUTED = new Color(156, 163, 175);
    private static final Color TEXT_CYAN = new Color(56, 189, 248);
    private static final Color TEXT_GOLD = new Color(251, 191, 36);

    private final Client client;
    private final OsrsMerchPlugin plugin;
    private final OsrsMerchConfig config;
    private final OsrsWikiPriceService priceService;
    private final ItemManager itemManager;
    private final GeInputHandler inputHandler;

    @Inject
    public OsrsMerchOverlay(
        Client client,
        OsrsMerchPlugin plugin,
        OsrsMerchConfig config,
        OsrsWikiPriceService priceService,
        ItemManager itemManager,
        GeInputHandler inputHandler
    ) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.priceService = priceService;
        this.itemManager = itemManager;
        this.inputHandler = inputHandler;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D g) {
        if (!plugin.isGeOfferSetupOpen()) {
            inputHandler.setOverlayActive(false);
            return null;
        }

        int itemId = plugin.getSelectedItemId();
        if (itemId <= 0) {
            inputHandler.setOverlayActive(false);
            return null;
        }

        Rectangle chatboxBounds = getChatboxBounds();
        if (chatboxBounds == null || chatboxBounds.width <= 0 || chatboxBounds.height <= 0) {
            inputHandler.setOverlayActive(false);
            return null;
        }

        inputHandler.setOverlayActive(true);
        inputHandler.clearButtons();

        // Enable high-quality anti-aliasing for text and shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int x = chatboxBounds.x;
        int y = chatboxBounds.y;
        int w = chatboxBounds.width;
        int h = chatboxBounds.height;

        renderBackground(g, x, y, w, h);

        ItemPriceData priceData = priceService.getPrice(itemId);
        ItemMapping mapping = priceService.getMapping(itemId);
        ItemVolumeData volumeData = priceService.getVolume(itemId);

        long now = System.currentTimeMillis() / 1000L;

        // Render Header
        int headerHeight = 34;
        renderHeader(g, itemId, mapping, priceData, x + 8, y + 6, w - 16, headerHeight, now);

        // Content Area Bounds
        int contentY = y + headerHeight + 8;
        int footerHeight = config.enableQuickButtons() ? 28 : 0;
        int contentH = h - headerHeight - footerHeight - 16;

        renderMetricsGrid(g, priceData, mapping, volumeData, x + 8, contentY, w - 16, contentH, now);

        // Render Bottom Quick Buttons
        if (config.enableQuickButtons()) {
            int footerY = y + h - footerHeight - 6;
            renderQuickActionButtons(g, priceData, x + 8, footerY, w - 16, footerHeight);
        }

        return new Dimension(w, h);
    }

    private Rectangle getChatboxBounds() {
        if (client == null) {
            return null;
        }

        // Try standard chatbox container, message layer, chat area, or input
        Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
        if (chatbox == null || chatbox.isHidden()) {
            chatbox = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        }
        if (chatbox == null || chatbox.isHidden()) {
            chatbox = client.getWidget(InterfaceID.Chatbox.CHATAREA);
        }
        if (chatbox == null || chatbox.isHidden()) {
            chatbox = client.getWidget(InterfaceID.Chatbox.INPUT);
        }

        if (chatbox != null && !chatbox.isHidden()) {
            return chatbox.getBounds();
        }

        return null;
    }

    private void renderBackground(Graphics2D g, int x, int y, int w, int h) {
        // Gradient fill
        GradientPaint bgGradient = new GradientPaint(x, y, BG_TOP, x, y + h, BG_BOTTOM);
        g.setPaint(bgGradient);
        g.fillRoundRect(x, y, w, h, 6, 6);

        // Outer & Accent Border
        g.setColor(BORDER_OUTER);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, w - 1, h - 1, 6, 6);

        // Top Accent Line
        g.setColor(config.accentColor());
        g.drawLine(x + 4, y + 1, x + w - 5, y + 1);
    }

    private void renderHeader(
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

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString(itemName, textX, y + 16);

        // Item Buy Limit Badge
        int limit = mapping != null ? mapping.getLimit() : 0;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        String limitStr = limit > 0 ? "4h Limit: " + GP_FORMAT.format(limit) : "4h Limit: None";
        g.setColor(TEXT_CYAN);
        g.drawString(limitStr, textX, y + 29);

        // Right-aligned Live Status & Freshness
        String statusStr;
        Color statusColor;
        if (priceData != null && (priceData.getHigh() > 0 || priceData.getLow() > 0)) {
            long newestAge = Math.min(
                priceData.getHighTime() > 0 ? now - priceData.getHighTime() : Long.MAX_VALUE,
                priceData.getLowTime() > 0 ? now - priceData.getLowTime() : Long.MAX_VALUE
            );
            statusStr = "● Real-Time (" + (newestAge < 60 ? newestAge + "s" : (newestAge / 60) + "m") + " ago)";
            statusColor = config.positiveProfitColor();
        } else {
            statusStr = "● No Live Trades";
            statusColor = TEXT_MUTED;
        }

        FontMetrics fm = g.getFontMetrics();
        int statusW = fm.stringWidth(statusStr);
        g.setColor(statusColor);
        g.drawString(statusStr, x + w - statusW - 2, y + 16);

        // Tax rate tag in header
        String taxTag = "Tax: " + config.taxPercentage() + "%";
        int taxW = fm.stringWidth(taxTag);
        g.setColor(TEXT_GOLD);
        g.drawString(taxTag, x + w - taxW - 2, y + 29);
    }

    private void renderMetricsGrid(
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
        renderPriceColumn(g, priceData, x + 6, y + 18, colWidth - 12, now);

        // Card 2: Margins & Profit
        int col2X = x + colWidth + colGap;
        renderCard(g, col2X, y, colWidth, h, "MARGIN & PROFIT");
        renderMarginColumn(g, priceData, mapping, col2X + 6, y + 18, colWidth - 12);

        // Card 3: Liquidity & 5m Volume
        int col3X = col2X + colWidth + colGap;
        renderCard(g, col3X, y, colWidth, h, "5M TRADE VELOCITY");
        renderVolumeColumn(g, volumeData, col3X + 6, y + 18, colWidth - 12);
    }

    private void renderCard(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(CARD_BG);
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(CARD_BORDER);
        g.drawRoundRect(x, y, w, h, 4, 4);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(TEXT_MUTED);
        g.drawString(title, x + 6, y + 12);
    }

    private void renderPriceColumn(
        Graphics2D g,
        ItemPriceData priceData,
        int x,
        int y,
        int w,
        long now
    ) {
        if (priceData == null) {
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
            g.setColor(TEXT_MUTED);
            g.drawString("Fetching prices...", x, y + 18);
            return;
        }

        // Insta-Buy (High / Ask)
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.setColor(TEXT_MUTED);
        g.drawString("Insta-Buy (Ask):", x, y + 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.setColor(Color.WHITE);
        String highStr = priceData.getHigh() > 0 ? GP_FORMAT.format(priceData.getHigh()) + " gp" : "None";
        g.drawString(highStr, x, y + 25);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        g.setColor(TEXT_MUTED);
        g.drawString(priceData.getHighAgeFormatted(now), x, y + 36);

        // Insta-Sell (Low / Bid)
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.setColor(TEXT_MUTED);
        g.drawString("Insta-Sell (Bid):", x, y + 50);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.setColor(Color.WHITE);
        String lowStr = priceData.getLow() > 0 ? GP_FORMAT.format(priceData.getLow()) + " gp" : "None";
        g.drawString(lowStr, x, y + 63);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        g.setColor(TEXT_MUTED);
        g.drawString(priceData.getLowAgeFormatted(now), x, y + 74);
    }

    private void renderMarginColumn(
        Graphics2D g,
        ItemPriceData priceData,
        ItemMapping mapping,
        int x,
        int y,
        int w
    ) {
        if (priceData == null || priceData.getHigh() <= 0 || priceData.getLow() <= 0) {
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
            g.setColor(TEXT_MUTED);
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
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.setColor(TEXT_MUTED);
        g.drawString("Net Margin:", x, y + 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        Color marginColor = net > 0 ? config.positiveProfitColor() : config.negativeProfitColor();
        g.setColor(marginColor);
        String netStr = (net > 0 ? "+" : "") + GP_FORMAT.format(net) + " gp";
        g.drawString(netStr, x, y + 26);

        // Tax deduction line
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        g.setColor(TEXT_GOLD);
        g.drawString("Tax: -" + GP_FORMAT.format(tax) + " gp (Gross: " + GP_FORMAT.format(gross) + ")", x, y + 38);

        // ROI %
        if (config.showRoi()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            g.drawString("ROI:", x, y + 52);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(marginColor);
            g.drawString(PERCENT_FORMAT.format(roi / 100.0), x + 28, y + 52);
        }

        // Limit Potential Profit
        if (config.showBuyLimitProfit() && mapping != null && mapping.getLimit() > 0) {
            long limitProfit = priceData.getPotentialLimitProfit(mapping.getLimit(), taxRate, taxCap);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            g.drawString("Limit Profit: ", x, y + 66);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(marginColor);
            g.drawString(formatShortGp(limitProfit), x, y + 78);
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
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            g.drawString("Volumes hidden in config", x, y + 20);
            return;
        }

        if (volumeData == null) {
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
            g.setColor(TEXT_MUTED);
            g.drawString("No 5m trades recorded", x, y + 20);
            return;
        }

        int bought = volumeData.getHighPriceVolume();
        int sold = volumeData.getLowPriceVolume();
        int total = volumeData.getTotalVolume();

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.setColor(TEXT_MUTED);
        g.drawString("5m Trades: ", x, y + 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString(GP_FORMAT.format(total) + " items", x + 60, y + 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.setColor(config.positiveProfitColor());
        g.drawString("▲ Bought: " + GP_FORMAT.format(bought), x, y + 28);

        g.setColor(TEXT_GOLD);
        g.drawString("▼ Sold: " + GP_FORMAT.format(sold), x, y + 42);

        // Liquidity Tag
        String liquidityRating;
        Color liqColor;
        if (total > 50) {
            liquidityRating = "HIGH LIQUIDITY";
            liqColor = config.positiveProfitColor();
        } else if (total > 10) {
            liquidityRating = "MODERATE LIQUIDITY";
            liqColor = TEXT_CYAN;
        } else if (total > 0) {
            liquidityRating = "LOW LIQUIDITY";
            liqColor = TEXT_GOLD;
        } else {
            liquidityRating = "ILLIQUID / SLOW";
            liqColor = config.negativeProfitColor();
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(liqColor);
        g.drawString("● " + liquidityRating, x, y + 66);
    }

    private void renderQuickActionButtons(
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
            "⚡ Bid (" + formatShortGp(instaSellPrice) + ")",
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
            "⚡ Outbid +" + buyOffset + " (" + formatShortGp(outbidPrice) + ")",
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
            "⚡ Undercut -" + sellOffset + " (" + formatShortGp(undercutPrice) + ")",
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
            "⚡ Ask (" + formatShortGp(instaBuyPrice) + ")",
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
        g.setColor(isHovered ? BTN_HOVER_BG : BTN_BG);
        g.fillRoundRect(x, y, w, h, 4, 4);

        g.setColor(isHovered ? config.accentColor() : BTN_BORDER);
        g.drawRoundRect(x, y, w, h, 4, 4);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.setColor(price > 0 ? (isHovered ? Color.WHITE : TEXT_GOLD) : TEXT_MUTED);

        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textX = x + Math.max(2, (w - textW) / 2);
        int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();

        g.drawString(label, textX, textY);
    }

    private String formatShortGp(long amount) {
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
}
