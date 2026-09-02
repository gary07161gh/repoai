package com.osrsmerch;

import com.osrsmerch.model.ItemMapping;
import com.osrsmerch.model.ItemPriceData;
import com.osrsmerch.model.ItemVolumeData;
import com.osrsmerch.model.TimeseriesDataPoint;
import com.osrsmerch.service.OsrsWikiPriceService;
import com.osrsmerch.ui.HeaderRenderer;
import com.osrsmerch.ui.MetricsGridRenderer;
import com.osrsmerch.ui.OverlayTheme;
import com.osrsmerch.ui.QuickButtonsRenderer;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
@Singleton
public class OsrsMerchOverlay extends Overlay {

    private final Client client;
    private final OsrsMerchPlugin plugin;
    private final OsrsMerchConfig config;
    private final OsrsWikiPriceService priceService;
    private final GeInputHandler inputHandler;
    private final HeaderRenderer headerRenderer;
    private final MetricsGridRenderer metricsGridRenderer;
    private final QuickButtonsRenderer quickButtonsRenderer;

    @Inject
    public OsrsMerchOverlay(
        Client client,
        OsrsMerchPlugin plugin,
        OsrsMerchConfig config,
        OsrsWikiPriceService priceService,
        GeInputHandler inputHandler,
        HeaderRenderer headerRenderer,
        MetricsGridRenderer metricsGridRenderer,
        QuickButtonsRenderer quickButtonsRenderer
    ) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.priceService = priceService;
        this.inputHandler = inputHandler;
        this.headerRenderer = headerRenderer;
        this.metricsGridRenderer = metricsGridRenderer;
        this.quickButtonsRenderer = quickButtonsRenderer;

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
        // Clear buttons at the start of each frame so we only register currently-rendered button bounds
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
        int headerHeight = 32;
        headerRenderer.renderHeader(g, itemId, mapping, priceData, x + 8, y + 5, w - 16, headerHeight, now);

        // Content Area Bounds
        int contentY = y + headerHeight + 5;
        int footerHeight = config.enableQuickButtons() ? 26 : 0;
        int contentH = h - headerHeight - footerHeight - 12;

        if (config.showPriceGraph()) {
            int stripH = 18;
            metricsGridRenderer.renderSummaryStrip(g, priceData, mapping, volumeData, x + 8, contentY, w - 16, stripH, now);

            int graphY = contentY + stripH + 3;
            int graphH = contentH - stripH - 3;

            if (graphH > 25) {
                PriceGraphRenderer graphRenderer = new PriceGraphRenderer(
                    config.instabuyLineColor(),
                    config.instasellLineColor()
                );
                List<TimeseriesDataPoint> points = priceService.getTimeseries(itemId);
                graphRenderer.render(g, points, x + 8, graphY, w - 16, graphH);
            }
        } else {
            metricsGridRenderer.renderMetricsGrid(g, priceData, mapping, volumeData, x + 8, contentY, w - 16, contentH, now);
        }

        // Render Bottom Quick Action Buttons
        if (config.enableQuickButtons()) {
            int footerY = y + h - footerHeight - 5;
            quickButtonsRenderer.renderQuickActionButtons(g, priceData, x + 8, footerY, w - 16, footerHeight);
        }

        return new Dimension(w, h);
    }

    private Rectangle getChatboxBounds() {
        if (client == null) {
            return null;
        }

        // Try standard chatbox candidate widgets
        Widget[] candidates = new Widget[] {
            client.getWidget(InterfaceID.Chatbox.UNIVERSE),
            client.getWidget(InterfaceID.Chatbox.CHATAREA),
            client.getWidget(InterfaceID.Chatbox.CHAT_BACKGROUND),
            client.getWidget(InterfaceID.Chatbox.MES_LAYER),
            client.getWidget(InterfaceID.Chatbox.INPUT)
        };

        for (Widget w : candidates) {
            if (w != null && !w.isHidden()) {
                Rectangle r = w.getBounds();
                if (r != null && r.width > 50 && r.height > 50) {
                    return r;
                }
            }
        }

        // Fallback: standard bottom-left chatbox region in OSRS (519x165 px)
        int canvasHeight = client.getCanvasHeight();
        int canvasWidth = client.getCanvasWidth();
        if (canvasHeight > 0 && canvasWidth > 0) {
            int w = Math.min(519, canvasWidth);
            int h = 165;
            int y = Math.max(0, canvasHeight - h);
            return new Rectangle(0, y, w, h);
        }

        return null;
    }

    private void renderBackground(Graphics2D g, int x, int y, int w, int h) {
        // Gradient fill
        GradientPaint bgGradient = new GradientPaint(x, y, OverlayTheme.BG_TOP, x, y + h, OverlayTheme.BG_BOTTOM);
        g.setPaint(bgGradient);
        g.fillRoundRect(x, y, w, h, 6, 6);

        // Outer & Accent Border
        g.setColor(OverlayTheme.BORDER_OUTER);
        g.setStroke(OverlayTheme.BORDER_STROKE);
        g.drawRoundRect(x, y, w - 1, h - 1, 6, 6);

        // Top Accent Line
        g.setColor(config.accentColor());
        g.drawLine(x + 4, y + 1, x + w - 5, y + 1);
    }
}
