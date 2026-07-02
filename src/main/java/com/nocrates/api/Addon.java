package com.nocrates.api;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.logging.Logger;

/**
 * Base class for noCrates addons — both the built-in modules and external jars dropped
 * into {@code plugins/noCrates/addons/} (with an addon.yml: name, main, version,
 * author). Lifecycle: onLoad -> onEnable -> onDisable.
 */
public abstract class Addon {

    private NoCratesApi api;
    private AddonDescription description;
    private Logger logger;
    private ConfigurationSection config = new MemoryConfiguration();
    private boolean initialized;

    /** Internal wiring — called once by the module manager. */
    public final void initialize(NoCratesApi api, AddonDescription description,
                                 Logger logger, ConfigurationSection config) {
        if (initialized) throw new IllegalStateException("Addon already initialized");
        this.api = api;
        this.description = description;
        this.logger = logger;
        if (config != null) this.config = config;
        this.initialized = true;
    }

    public void onLoad() {
    }

    public abstract void onEnable();

    public void onDisable() {
    }

    public final NoCratesApi api() {
        return api;
    }

    public final AddonDescription description() {
        return description;
    }

    public final Logger logger() {
        return logger;
    }

    /** The addon's section of modules.yml (by addon name), never null. */
    public final ConfigurationSection config() {
        return config;
    }
}
