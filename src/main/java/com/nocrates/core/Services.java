package com.nocrates.core;

import com.nocrates.NoCrates;
import com.nocrates.gui.MenuListener;
import com.nocrates.message.Messages;

/**
 * Central service locator. Holds the long-lived managers for the plugin.
 * Fields are populated in {@link #start()} as each subsystem is implemented;
 * {@link #shutdown()} tears them down in reverse order.
 */
public final class Services {

    private final NoCrates plugin;
    private final Config config;
    private Messages messages;

    public Services(NoCrates plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Construct and register managers. Extended as subsystems come online. */
    public void start() {
        this.messages = new Messages(plugin);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
    }

    /** Flush and release resources. */
    public void shutdown() {
        // datastore flush, hologram cleanup, etc. (wired in later tasks)
    }

    public NoCrates plugin() {
        return plugin;
    }

    public Config config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }
}
