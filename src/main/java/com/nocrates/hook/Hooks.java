package com.nocrates.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** Soft-dependency detection. All access is guarded so no hook is ever required. */
public final class Hooks {

    private static boolean papi;
    private static boolean vault;
    private static boolean itemsAdder;
    private static boolean oraxen;
    private static boolean nexo;
    private static boolean mmoItems;

    private Hooks() {
    }

    public static void detect() {
        papi = present("PlaceholderAPI");
        vault = present("Vault");
        itemsAdder = present("ItemsAdder");
        oraxen = present("Oraxen");
        nexo = present("Nexo");
        mmoItems = present("MMOItems");
    }

    private static boolean present(String plugin) {
        return Bukkit.getPluginManager().getPlugin(plugin) != null;
    }

    public static boolean papi() {
        return papi;
    }

    public static boolean vault() {
        return vault;
    }

    public static boolean itemsAdder() {
        return itemsAdder;
    }

    public static boolean oraxen() {
        return oraxen;
    }

    public static boolean nexo() {
        return nexo;
    }

    public static boolean mmoItems() {
        return mmoItems;
    }

    /** Applies PlaceholderAPI placeholders when installed; returns input untouched otherwise. */
    public static String papiApply(OfflinePlayer player, String input) {
        if (!papi || input == null || input.indexOf('%') < 0) return input;
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, input);
        } catch (Throwable t) {
            return input;
        }
    }
}
