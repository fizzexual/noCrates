package com.nocrates.core;

import com.nocrates.NoCrates;
import org.bukkit.configuration.file.FileConfiguration;

/** Typed accessor over {@code config.yml}. */
public final class Config {

    private final NoCrates plugin;
    private FileConfiguration raw;

    public Config(NoCrates plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.raw = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return raw;
    }

    public String storageType() {
        return raw.getString("storage.type", "yaml").toLowerCase();
    }

    public boolean debug() {
        return raw.getBoolean("settings.debug", false);
    }

    public int saveIntervalSeconds() {
        return raw.getInt("settings.save-interval-seconds", 300);
    }

    public String defaultOpenSound() {
        return raw.getString("settings.default-open-sound", "BLOCK_NOTE_BLOCK_PLING");
    }
}
