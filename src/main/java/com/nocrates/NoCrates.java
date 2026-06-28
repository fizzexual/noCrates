package com.nocrates;

import com.nocrates.core.Config;
import com.nocrates.core.Services;
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
        getLogger().info("noCrates enabled on " + getServer().getBukkitVersion());
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.shutdown();
        }
        getLogger().info("noCrates disabled.");
    }

    public Services services() {
        return services;
    }
}
