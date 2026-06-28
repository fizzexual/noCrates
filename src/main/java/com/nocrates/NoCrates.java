package com.nocrates;

import com.nocrates.command.CrateCommand;
import com.nocrates.command.CratesCommand;
import com.nocrates.core.Config;
import com.nocrates.core.Services;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point. Wires services during {@link #onEnable()} and flushes
 * state on {@link #onDisable()}. Managers are created and registered through
 * {@link Services} as each subsystem comes online.
 */
public final class NoCrates extends JavaPlugin {

    private Services services;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Config config = new Config(this);
        this.services = new Services(this, config);
        this.services.start();
        registerCommands();
        getLogger().info("noCrates enabled on " + getServer().getBukkitVersion());
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.shutdown();
        }
        getLogger().info("noCrates disabled.");
    }

    private void registerCommands() {
        CrateCommand crateCommand = new CrateCommand(this);
        PluginCommand crate = getCommand("crate");
        if (crate != null) {
            crate.setExecutor(crateCommand);
            crate.setTabCompleter(crateCommand);
        }
        CratesCommand cratesCommand = new CratesCommand(this);
        PluginCommand crates = getCommand("crates");
        if (crates != null) {
            crates.setExecutor(cratesCommand);
            crates.setTabCompleter(cratesCommand);
        }
    }

    public Services services() {
        return services;
    }
}
