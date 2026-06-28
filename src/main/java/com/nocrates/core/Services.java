package com.nocrates.core;

import com.nocrates.NoCrates;
import com.nocrates.animation.AnimationRegistry;
import com.nocrates.block.CrateBlockManager;
import com.nocrates.chesthunt.ChestHuntManager;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.gui.MenuListener;
import com.nocrates.hook.Hooks;
import com.nocrates.hook.PlaceholderHook;
import com.nocrates.key.KeyManager;
import com.nocrates.message.Messages;
import com.nocrates.open.OpenController;
import com.nocrates.reward.RarityRegistry;
import com.nocrates.storage.DataStore;
import com.nocrates.storage.PlayerDataManager;
import com.nocrates.storage.YamlDataStore;

/**
 * Central service locator. Constructs and holds the long-lived managers, wires
 * them in dependency order in {@link #start()}, and tears them down in
 * {@link #shutdown()}.
 */
public final class Services {

    private final NoCrates plugin;
    private final Config config;

    private Messages messages;
    private RarityRegistry rarities;
    private DataStore dataStore;
    private PlayerDataManager playerData;
    private CrateRegistry crates;
    private KeyManager keys;
    private AnimationRegistry animations;
    private OpenController openController;
    private CrateBlockManager crateBlocks;
    private ChestHuntManager chestHunt;

    public Services(NoCrates plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        this.messages = new Messages(plugin);
        Hooks.init(plugin);
        this.rarities = new RarityRegistry(plugin);
        this.dataStore = new YamlDataStore(plugin);
        this.playerData = new PlayerDataManager(plugin, dataStore);
        this.playerData.start();
        this.crates = new CrateRegistry(plugin);
        this.crates.loadAll();
        this.keys = new KeyManager(plugin, playerData);
        this.animations = new AnimationRegistry(plugin);
        this.openController = new OpenController(plugin, animations);
        this.crateBlocks = new CrateBlockManager(plugin);
        this.crateBlocks.start();
        this.chestHunt = new ChestHuntManager(plugin);
        this.chestHunt.start();
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        registerPlaceholders();
        startMetrics();
    }

    private void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new PlaceholderHook(plugin).register();
            plugin.getLogger().info("Hooked into PlaceholderAPI.");
        } catch (Throwable t) {
            plugin.getLogger().warning("PlaceholderAPI hook failed: " + t.getMessage());
        }
    }

    private void startMetrics() {
        int id = config.raw().getInt("settings.bstats-id", 0);
        if (id <= 0) {
            return;
        }
        try {
            new org.bstats.bukkit.Metrics(plugin, id);
        } catch (Throwable t) {
            plugin.getLogger().warning("bStats metrics failed: " + t.getMessage());
        }
    }

    /** Re-read configuration files without a restart. */
    public void reload() {
        config.reload();
        messages.reload();
        rarities.load(plugin);
        crates.reload();
        if (crateBlocks != null) {
            crateBlocks.refresh();
        }
    }

    public void shutdown() {
        if (chestHunt != null) {
            chestHunt.shutdown();
        }
        if (crateBlocks != null) {
            crateBlocks.shutdown();
        }
        if (playerData != null) {
            playerData.shutdown();
        }
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

    public RarityRegistry rarities() {
        return rarities;
    }

    public PlayerDataManager playerData() {
        return playerData;
    }

    public CrateRegistry crates() {
        return crates;
    }

    public KeyManager keys() {
        return keys;
    }

    public AnimationRegistry animations() {
        return animations;
    }

    public OpenController openController() {
        return openController;
    }

    public CrateBlockManager crateBlocks() {
        return crateBlocks;
    }

    public ChestHuntManager chestHunt() {
        return chestHunt;
    }
}
