package com.nocrates.core;

import com.nocrates.NoCrates;

/**
 * Central service locator. Holds the long-lived managers for the plugin.
 * Fields are populated in {@link #start()} as each subsystem is implemented;
 * {@link #shutdown()} tears them down in reverse order.
 */
public final class Services {

    private final NoCrates plugin;
    private final Config config;

    public Services(NoCrates plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Construct and register managers. Extended as subsystems come online. */
    public void start() {
        // Wired incrementally: VersionCompat, Messages, registries, datastore, etc.
    }

    /** Flush and release resources. */
    public void shutdown() {
        // datastore flush, hologram cleanup, etc.
    }

    public NoCrates plugin() {
        return plugin;
    }

    public Config config() {
        return config;
    }
}
