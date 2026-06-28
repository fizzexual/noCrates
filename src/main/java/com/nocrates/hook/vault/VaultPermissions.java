package com.nocrates.hook.vault;

import com.nocrates.hook.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault-backed permissions. Loaded only when Vault is installed (guarded in
 * {@link com.nocrates.hook.Hooks#init}).
 */
public final class VaultPermissions implements Permissions {

    private final net.milkbowl.vault.permission.Permission permission;

    public VaultPermissions() {
        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        this.permission = rsp != null ? rsp.getProvider() : null;
    }

    @Override
    public boolean available() {
        return permission != null;
    }

    @Override
    public void add(Player player, String node) {
        if (permission != null) {
            permission.playerAdd(player, node);
        }
    }
}
