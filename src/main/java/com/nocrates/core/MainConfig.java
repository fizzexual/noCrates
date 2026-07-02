package com.nocrates.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Typed access to config.yml. */
public final class MainConfig {

    private final JavaPlugin plugin;
    private FileConfiguration c;

    public MainConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.c = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.c = plugin.getConfig();
    }

    public String language() {
        return c.getString("language", "en_US");
    }

    public int renderRadius() {
        return Math.max(8, c.getInt("render-radius", 48));
    }

    public boolean quickOpenEnabled() {
        return c.getBoolean("open.quick-open-enabled", true);
    }

    public double knockback() {
        return c.getDouble("open.knockback", 0.4);
    }

    public String databaseType() {
        return c.getString("database.type", "YAML").toUpperCase(java.util.Locale.ROOT);
    }

    public String tablePrefix() {
        return c.getString("database.table-prefix", "nc_");
    }

    public String mysql(String key, String def) {
        return c.getString("database.mysql." + key, def);
    }

    public int mysqlInt(String key, int def) {
        return c.getInt("database.mysql." + key, def);
    }

    public boolean mysqlSsl() {
        return c.getBoolean("database.mysql.ssl", false);
    }

    public boolean logFile() {
        return c.getBoolean("logging.file", true);
    }

    public boolean discordEnabled() {
        return c.getBoolean("logging.discord.enabled", false);
    }

    public String discordWebhook() {
        return c.getString("logging.discord.webhook-url", "");
    }

    public String discordUsername() {
        return c.getString("logging.discord.username", "noCrates");
    }

    public boolean discordLogs(String event) {
        return c.getBoolean("logging.discord.events." + event, false);
    }

    public boolean metrics() {
        return c.getBoolean("metrics", true);
    }

    public boolean debug() {
        return c.getBoolean("debug", false);
    }
}
