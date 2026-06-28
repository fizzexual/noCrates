package com.nocrates.hook.vault;

import com.nocrates.hook.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault-backed economy. Loaded only when Vault is installed (guarded in
 * {@link com.nocrates.hook.Hooks#init}), so the Vault type reference here is safe.
 */
public final class VaultEconomy implements Economy {

    private final net.milkbowl.vault.economy.Economy economy;

    public VaultEconomy() {
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        this.economy = rsp != null ? rsp.getProvider() : null;
    }

    @Override
    public boolean available() {
        return economy != null;
    }

    @Override
    public void deposit(Player player, double amount) {
        if (economy != null && amount > 0) {
            economy.depositPlayer(player, amount);
        }
    }
}
