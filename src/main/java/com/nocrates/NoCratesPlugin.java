package com.nocrates;

import com.nocrates.action.Actions;
import com.nocrates.compat.Scheduling;
import com.nocrates.compat.ServerVersion;
import com.nocrates.core.MainConfig;
import com.nocrates.core.Services;
import com.nocrates.hook.CustomItems;
import com.nocrates.hook.Hooks;
import com.nocrates.logging.ActionLogger;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.MenuConfig;
import com.nocrates.menu.MenuListener;
import com.nocrates.storage.DataStore;
import com.nocrates.storage.MySqlDataStore;
import com.nocrates.storage.PlayerCache;
import com.nocrates.storage.SqliteDataStore;
import com.nocrates.storage.YamlDataStore;
import com.nocrates.text.Lang;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class NoCratesPlugin extends JavaPlugin {

    public static final String[] LANGUAGES = {
            "en_US", "de_DE", "fr_FR", "es_ES", "pt_BR", "ru_RU", "zh_CN", "pl_PL", "tr_TR", "nl_NL"
    };

    private Services services;
    private Scheduling.Cancellable flushTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultResource("modules.yml");
        saveDefaultResource("keys.yml");
        saveDefaultResource("rarities.yml");
        saveDefaultResource("crates/example.yml");
        for (String menu : MenuConfig.DEFAULTS) saveDefaultResource("menus/" + menu + ".yml");
        for (String code : LANGUAGES) saveDefaultResource("languages/" + code + ".yml");

        MainConfig config = new MainConfig(this);
        Lang lang = new Lang(this, config.language());
        this.services = new Services(this, config, lang);

        Hooks.detect();
        CustomItems.registerDefaults();

        services.menus(new MenuConfig(this));
        services.reloads().register(services.menus());
        services.actions(new Actions(this));
        services.dataStore(createDataStore(config));
        services.players(new PlayerCache(services.dataStore()));
        services.actionLogger(new ActionLogger(this, config));

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ChatPrompt(), this);
        getServer().getPluginManager().registerEvents(services.players(), this);

        // Periodic dirty-data flush (every 5 minutes).
        flushTask = Scheduling.asyncTimer(this, 20L * 300, 20L * 300, services.players()::flushDirty);

        getLogger().info("noCrates " + getDescription().getVersion() + " on MC " + ServerVersion.display()
                + (Scheduling.FOLIA ? " (Folia)" : "") + " — storage " + config.databaseType()
                + ", language " + config.language());
    }

    @Override
    public void onDisable() {
        if (flushTask != null) flushTask.cancel();
        if (services != null) {
            if (services.players() != null) services.players().flushAllSync();
            if (services.dataStore() != null) services.dataStore().close();
            if (services.actionLogger() != null) services.actionLogger().close();
        }
    }

    private DataStore createDataStore(MainConfig config) {
        String type = config.databaseType();
        try {
            if (type.equals("MYSQL") || type.equals("MARIADB")) {
                return new MySqlDataStore(this, config);
            }
            if (type.equals("SQLITE")) {
                if (SqliteDataStore.driverPresent()) {
                    return new SqliteDataStore(this, config.tablePrefix());
                }
                getLogger().warning("SQLite driver not present on this server — falling back to YAML storage.");
            }
        } catch (Exception e) {
            getLogger().severe("Could not initialise " + type + " storage (" + e.getMessage() + ") — falling back to YAML.");
        }
        return new YamlDataStore(this);
    }

    public Services services() {
        return services;
    }

    /** saveResource without the noisy overwrite log, skipping when present or unshipped. */
    public void saveDefaultResource(String path) {
        File out = new File(getDataFolder(), path);
        if (out.exists() || getResource(path) == null) return;
        saveResource(path, false);
    }
}
