package com.osrsmerch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OsrsMerchPluginLauncher {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(OsrsMerchPlugin.class);
        RuneLite.main(args);
    }
}
