package com.osrsmerch;

import com.google.inject.Provides;
import com.osrsmerch.service.OsrsWikiPriceService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@Singleton
@PluginDescriptor(
    name = "OSRS Merch Overlay",
    description = "Real-time Grand Exchange buy/sell prices, margins, 2% tax, and volume overlay over the chatbox",
    tags = {"grand exchange", "ge", "merch", "flipping", "prices", "overlay", "tax"}
)
public class OsrsMerchPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private OsrsMerchConfig config;

    @Inject
    private OsrsMerchOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private GeInputHandler inputHandler;

    @Inject
    private OsrsWikiPriceService priceService;

    @Getter
    private int selectedItemId = -1;

    @Getter
    private boolean isGeOfferSetupOpen = false;

    @Provides
    OsrsMerchConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OsrsMerchConfig.class);
    }

    @Override
    protected void startUp() {
        log.info("OSRS Merch Plugin starting up...");
        overlayManager.add(overlay);
        mouseManager.registerMouseListener(inputHandler);
        priceService.start();
    }

    @Override
    protected void shutDown() {
        log.info("OSRS Merch Plugin shutting down...");
        overlayManager.remove(overlay);
        mouseManager.unregisterMouseListener(inputHandler);
        priceService.shutdown();
        selectedItemId = -1;
        isGeOfferSetupOpen = false;
        inputHandler.setOverlayActive(false);
        inputHandler.clearButtons();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.GRAND_EXCHANGE) {
            checkGeState();
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.GRAND_EXCHANGE) {
            isGeOfferSetupOpen = false;
            selectedItemId = -1;
            inputHandler.setOverlayActive(false);
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        checkGeState();
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        // Script 779 / 80 / 84 are fired when GE item setup updates
        checkGeState();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!"osrsmerch".equals(event.getGroup())) {
            return;
        }

        if ("refreshIntervalSeconds".equals(event.getKey())) {
            priceService.restartScheduler();
        }
    }

    private void checkGeState() {
        if (client == null) {
            return;
        }

        Widget geOfferContainer = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
        if (geOfferContainer != null && !geOfferContainer.isHidden()) {
            isGeOfferSetupOpen = true;

            int itemId = client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
            if (itemId > 0) {
                this.selectedItemId = itemId;
            } else {
                // Fallback check on offer setup item widget
                Widget itemWidget = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
                if (itemWidget != null && itemWidget.getItemId() > 0) {
                    this.selectedItemId = itemWidget.getItemId();
                }
            }
        } else {
            isGeOfferSetupOpen = false;
            selectedItemId = -1;
        }
    }
}
