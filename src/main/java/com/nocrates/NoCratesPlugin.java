package com.nocrates;

import com.nocrates.action.Actions;
import com.nocrates.compat.Scheduling;
import com.nocrates.compat.ServerVersion;
import com.nocrates.core.MainConfig;
import com.nocrates.core.Services;
import com.nocrates.hook.CustomItems;
import com.nocrates.hook.Hooks;
import com.nocrates.hook.VaultHook;
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
    private com.nocrates.animation.IdleEffectTask idleEffects;
    private com.nocrates.module.ModuleManager modules;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultResource("modules.yml");
        saveDefaultResource("keys.yml");
        saveDefaultResource("rarities.yml");
        for (String crate : new String[]{"example", "shop", "legendary", "hunt", "daily", "showcase"}) {
            saveDefaultResource("crates/" + crate + ".yml");
        }
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

        services.keys(new com.nocrates.key.KeyRegistry(this));
        services.keyService(new com.nocrates.key.KeyService(services.keys(), services.players()));
        services.crates(new com.nocrates.crate.CrateRegistry(this));
        services.placements(new com.nocrates.crate.PlacementManager(this, services.crates()));
        services.animations(new com.nocrates.animation.AnimationService(this));
        com.nocrates.animation.BuiltinAnimations.registerAll(services.animations());
        this.idleEffects = new com.nocrates.animation.IdleEffectTask(this, services.animations());
        services.winLimits(new com.nocrates.reward.WinLimitService(services.dataStore()));
        services.openService(new com.nocrates.open.OpenService());
        services.rerolls(new com.nocrates.reroll.RerollService(services.players()));
        services.stats(new com.nocrates.stats.StatsService(services.players()));

        VaultHook.detect();
        services.actions().crateOpener((player, crateId) -> {
            var crate = services.crates().get(crateId);
            if (crate != null) services.openService().attempt(player, crate, null, false);
        });

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ChatPrompt(), this);
        getServer().getPluginManager().registerEvents(services.players(), this);
        getServer().getPluginManager().registerEvents(services.placements(), this);
        getServer().getPluginManager().registerEvents(
                new com.nocrates.crate.CrateClickListener(services.placements()), this);

        var command = getCommand("crates");
        if (command != null) {
            var executor = new com.nocrates.command.CratesCommand();
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        if (Hooks.papi()) {
            new com.nocrates.hook.PapiExpansion().register();
        }

        com.nocrates.editor.EditorHub.register();
        com.nocrates.migrate.Migrations.registerDefaults();

        this.modules = new com.nocrates.module.ModuleManager(this, new com.nocrates.module.ApiImpl());
        modules.enableAll();

        // bStats: 0 disables until a plugin id is registered on bstats.org.
        final int bstatsId = 0;
        if (config.metrics() && bstatsId > 0) {
            new org.bstats.bukkit.Metrics(this, bstatsId);
        }

        // World data becomes available after all plugins load.
        Scheduling.run(this, null, () -> {
            services.placements().rebuild();
            for (var crate : services.crates().all()) services.winLimits().warm(crate.id());
        });
        idleEffects.start();
        services.reloads().register(idleEffects::invalidate);

        // Periodic dirty-data flush (every 5 minutes).
        flushTask = Scheduling.asyncTimer(this, 20L * 300, 20L * 300, services.players()::flushDirty);

        getLogger().info("noCrates " + getDescription().getVersion() + " on MC " + ServerVersion.display()
                + (Scheduling.FOLIA ? " (Folia)" : "") + " — storage " + config.databaseType()
                + ", language " + config.language());
    }

    @Override
    public void onDisable() {
        if (flushTask != null) flushTask.cancel();
        if (idleEffects != null) idleEffects.stop();
        if (modules != null) modules.disableAll();
        if (services != null) {
            if (services.placements() != null) services.placements().shutdown();
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
