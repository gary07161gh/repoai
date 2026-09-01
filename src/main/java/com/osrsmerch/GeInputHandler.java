package com.osrsmerch;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseAdapter;

@Slf4j
@Singleton
public class GeInputHandler extends MouseAdapter {

    public enum ButtonType {
        SET_BUY_INSTA,
        SET_BUY_OUTBID,
        SET_SELL_INSTA,
        SET_SELL_UNDERCUT,
        REFRESH_DATA
    }

    private final Client client;
    private final ClientThread clientThread;

    private final Map<ButtonType, Rectangle> buttonBounds = new HashMap<>();
    private final Map<ButtonType, Integer> buttonPrices = new HashMap<>();

    @Getter
    @Setter
    private ButtonType hoveredButton = null;

    @Getter
    @Setter
    private boolean overlayActive = false;

    @Inject
    public GeInputHandler(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    public void registerButton(ButtonType type, Rectangle bounds, int price) {
        buttonBounds.put(type, bounds);
        buttonPrices.put(type, price);
    }

    public void clearButtons() {
        buttonBounds.clear();
        buttonPrices.clear();
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e) {
        if (!overlayActive || buttonBounds.isEmpty()) {
            hoveredButton = null;
            return e;
        }

        Point p = e.getPoint();
        ButtonType matched = null;
        for (Map.Entry<ButtonType, Rectangle> entry : buttonBounds.entrySet()) {
            if (entry.getValue().contains(p)) {
                matched = entry.getKey();
                break;
            }
        }

        hoveredButton = matched;
        return e;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        if (!overlayActive || e.getButton() != MouseEvent.BUTTON1 || buttonBounds.isEmpty()) {
            return e;
        }

        Point p = e.getPoint();
        for (Map.Entry<ButtonType, Rectangle> entry : buttonBounds.entrySet()) {
            if (entry.getValue().contains(p)) {
                ButtonType type = entry.getKey();
                Integer price = buttonPrices.get(type);
                handleButtonClick(type, price != null ? price : 0);
                e.consume();
                return e;
            }
        }

        return e;
    }

    private void handleButtonClick(ButtonType type, int price) {
        if (type == ButtonType.REFRESH_DATA) {
            log.debug("Manual refresh button clicked.");
            playClickFeedback();
            return;
        }

        if (price <= 0) {
            return;
        }

        log.debug("Applying price {} for button {}", price, type);
        playClickFeedback();

        clientThread.invokeLater(() -> {
            applyPriceToGeOffer(price);
        });
    }

    /**
     * Injects or sets the price on the active Grand Exchange offer window.
     */
    public void applyPriceToGeOffer(int price) {
        if (client == null) {
            return;
        }

        Widget geOfferContainer = client.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER);
        if (geOfferContainer == null || geOfferContainer.isHidden()) {
            return;
        }

        try {
            // Copy to clipboard as helper
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(String.valueOf(price)), null);

            // Script 80 / 84 / 779 are standard GE price adjustment scripts in OSRS
            // Try standard RuneLite script execution for GE price
            // ScriptID: 80 (ge_set_price), script params: [price]
            client.runScript(80, price);
        } catch (Exception ex) {
            log.debug("Script 80 invocation fallback: {}", ex.getMessage());
        }
    }

    private void playClickFeedback() {
        try {
            if (client != null) {
                client.playSoundEffect(SoundEffectID.UI_BOOP);
            }
        } catch (Exception ignored) {
        }
    }
}
