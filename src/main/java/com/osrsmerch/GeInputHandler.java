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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseAdapter;

@Slf4j
@Singleton
public class GeInputHandler extends MouseAdapter {

    public enum ButtonType {
        SET_BUY_INSTA,
        SET_BUY_OUTBID,
        SET_SELL_INSTA,
        SET_SELL_UNDERCUT
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

    @Getter
    private int lastCopiedPrice = 0;

    @Getter
    private long lastCopiedTime = 0L;

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
        if (price <= 0) {
            return;
        }

        log.debug("Applying price {} for button {}", price, type);
        playClickFeedback();

        clientThread.invokeLater(() -> applyPriceToGeOffer(price));
    }

    /**
     * Copies price to clipboard and attempts to inject into the active Grand Exchange offer window.
     */
    public void applyPriceToGeOffer(int price) {
        if (client == null || !overlayActive || price <= 0) {
            return;
        }

        this.lastCopiedPrice = price;
        this.lastCopiedTime = System.currentTimeMillis();

        try {
            // Copy to clipboard as convenient helper
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(String.valueOf(price)), null);

            // Attempt to trigger GE price setup script
            client.runScript(80, price);
        } catch (Exception ex) {
            log.trace("GE script execution fallback: {}", ex.getMessage());
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
