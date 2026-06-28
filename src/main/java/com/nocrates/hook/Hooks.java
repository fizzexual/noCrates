package com.nocrates.hook;

import com.nocrates.hook.vault.VaultEconomy;
import com.nocrates.hook.vault.VaultPermissions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Central access point for optional integrations. Vault-backed implementations
 * are only instantiated when Vault is present, so the classes that reference
 * Vault types are never loaded otherwise (avoids {@code NoClassDefFoundError}).
 */
public final class Hooks {

    private static Economy economy = new NoEconomy();
    private static Permissions permissions = new NoPermissions();

    private Hooks() {
    }

    public static void init(Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Economy vault = new VaultEconomy();
            if (vault.available()) {
                economy = vault;
                plugin.getLogger().info("Hooked into Vault economy.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Vault economy hook failed: " + t.getMessage());
        }
        try {
            Permissions vault = new VaultPermissions();
            if (vault.available()) {
                permissions = vault;
                plugin.getLogger().info("Hooked into Vault permissions.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Vault permission hook failed: " + t.getMessage());
        }
    }

    public static Economy economy() {
        return economy;
    }

    public static Permissions permissions() {
        return permissions;
    }

    private static final class NoEconomy implements Economy {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public void deposit(Player player, double amount) {
        }
    }

    private static final class NoPermissions implements Permissions {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public void add(Player player, String node) {
        }
    }
}
