# OSRS Merch Overlay - RuneLite Plugin

A high-performance, real-time Grand Exchange overlay for RuneLite that seamlessly replaces/overlays the chatbox with live market data whenever an item is selected in the Grand Exchange offer creation window.

---

## ✨ Features

* **Smart Chatbox Overlay:** Renders directly over the chatbox area when creating or modifying a Grand Exchange buy/sell offer. Leaves the chatbox completely untouched when outside of GE offer setup.
* **Live OSRS Wiki Pricing:** Asynchronously fetches real-time Instant-Buy (Ask) and Instant-Sell (Bid) prices with age/freshness timestamps (e.g. `12s ago`).
* **2% Grand Exchange Tax Calculation:** Automatically computes the updated 2% OSRS Grand Exchange tax (with 5,000,000 GP cap support) and accurate net profit per item.
* **Complete Margin & ROI Analytics:**
  * **Gross Margin:** `High Price - Low Price`
  * **GE Tax (2%):** Sale tax deduction
  * **Net Margin:** Realized profit per item after tax
  * **ROI %:** Return on investment percentage
  * **4-Hour Buy Limit Profit:** Total theoretical flip profit for the item's 4-hour buy limit
* **5-Minute Trade Velocity:** Real-time volume inflow (insta-buys) and outflow (insta-sells) along with liquidity rating badges (`HIGH`, `MODERATE`, `LOW`).
* **Interactive Quick-Action Price Buttons:**
  * `[⚡ Bid (Low)]`: Sets price to current Instant-Sell
  * `[⚡ Outbid +1 GP]`: Sets price to Instant-Sell + configurable outbid offset
  * `[⚡ Undercut -1 GP]`: Sets price to Instant-Buy - configurable undercut offset
  * `[⚡ Ask (High)]`: Sets price to current Instant-Buy
* **Full In-Game Configuration:** Customize tax rate (default 2%), tax cap, polling interval, GP offsets, volume toggles, and UI accent colors directly from RuneLite's Plugin Configuration panel.

---

## 🛠️ Project Structure

```
OSRS MERCH/
├── build.gradle
├── settings.gradle
├── gradlew & gradlew.bat
└── src/
    ├── main/
    │   └── java/com/osrsmerch/
    │       ├── OsrsMerchPlugin.java           # Plugin lifecycle, GE widget listeners
    │       ├── OsrsMerchConfig.java           # Plugin settings & color palette
    │       ├── OsrsMerchOverlay.java          # Chatbox canvas overlay rendering
    │       ├── GeInputHandler.java            # Mouse interaction & GE price injection
    │       ├── model/
    │       │   ├── ItemPriceData.java         # Prices, tax, margin & ROI formulas
    │       │   ├── ItemMapping.java           # Item names, limits, and store values
    │       │   └── ItemVolumeData.java        # 5m/1h volume velocity metrics
    │       └── service/
    │           └── OsrsWikiPriceService.java  # Async HTTP polling & memory cache
    └── test/
        └── java/com/osrsmerch/
            ├── OsrsMerchPluginLauncher.java   # Local RuneLite test launcher
            ├── MarginCalculationTest.java     # 2% tax & margin test suite
            └── OsrsWikiPriceServiceTest.java  # API integration & parsing tests
```

---

## 🚀 How to Run & Develop

### Prerequisites
* Java 11 or Java 17 JDK
* RuneLite client (or runs directly via Gradle test harness)

### Launching the Plugin in RuneLite Dev Client
To launch a local RuneLite test client with the plugin active:
```powershell
.\gradlew run
```
Or run `.\run.bat` / `.\run.ps1` from your terminal.

### Building the Distribution JAR
```powershell
.\gradlew build
```
The compiled plugin JAR will be generated at `build/libs/osrs-merch-1.0.0.jar`.

---

## ⚙️ Configuration Options

| Setting | Default | Description |
| :--- | :--- | :--- |
| **GE Tax Rate (%)** | `2.0%` | Tax percentage applied to sales |
| **GE Tax Cap (GP)** | `5,000,000` | Maximum tax charged per transaction |
| **Outbid Offset** | `+1 GP` | Added to Insta-Sell to outbid buy offers |
| **Undercut Offset** | `-1 GP` | Subtracted from Insta-Buy to undercut sell offers |
| **Show Trade Volumes** | `true` | Display 5m trade volume and liquidity ratings |
| **Show ROI %** | `true` | Display Return on Investment percentage |
| **Show Limit Profit** | `true` | Display theoretical profit for 4-hour buy limit |
| **Refresh Interval** | `10s` | Background polling rate for OSRS Wiki API |
| **Custom User-Agent** | `RuneLite-OsrsMerchOverlay` | Custom HTTP User-Agent header for Wiki API |
