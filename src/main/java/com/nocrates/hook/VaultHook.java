package com.nocrates.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault economy access for open costs and money-style rewards. */
public final class VaultHook {

    private static Economy economy;

    private VaultHook() {
    }

    public static void detect() {
        economy = null;
        if (!Hooks.vault()) return;
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        } catch (Throwable ignored) {
        }
    }

    public static boolean ready() {
        return economy != null;
    }

    public static boolean has(OfflinePlayer player, double amount) {
        return ready() && economy.has(player, amount);
    }

    public static boolean withdraw(OfflinePlayer player, double amount) {
        return ready() && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static void deposit(OfflinePlayer player, double amount) {
        if (ready()) economy.depositPlayer(player, amount);
    }

    public static String format(double amount) {
        return ready() ? economy.format(amount) : String.valueOf(amount);
    }
}
