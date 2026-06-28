package com.nocrates.core;

import com.nocrates.NoCrates;
import com.nocrates.animation.AnimationRegistry;
import com.nocrates.block.CrateBlockManager;
import com.nocrates.chesthunt.ChestHuntManager;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.editor.ChatPrompts;
import com.nocrates.gui.MenuListener;
import com.nocrates.hook.Hooks;
import com.nocrates.hook.PlaceholderHook;
import com.nocrates.key.KeyManager;
import com.nocrates.message.Messages;
import com.nocrates.open.OpenController;
import com.nocrates.reward.RarityRegistry;
import com.nocrates.storage.DataStore;
import com.nocrates.storage.MySqlDataStore;
import com.nocrates.storage.PlayerDataManager;
import com.nocrates.storage.YamlDataStore;
import org.bukkit.configuration.ConfigurationSection;

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
    private ChatPrompts chatPrompts;

    public Services(NoCrates plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        this.messages = new Messages(plugin);
        Hooks.init(plugin);
        this.rarities = new RarityRegistry(plugin);
        this.dataStore = createDataStore();
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
        this.chatPrompts = new ChatPrompts(plugin);
        this.chatPrompts.start();
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

    private DataStore createDataStore() {
        if ("mysql".equalsIgnoreCase(config.storageType())) {
            try {
                ConfigurationSection mysql = config.raw().getConfigurationSection("storage.mysql");
                if (mysql == null) {
                    mysql = config.raw().createSection("storage.mysql");
                }
                DataStore store = new MySqlDataStore(plugin, mysql);
                plugin.getLogger().info("Using MySQL storage.");
                return store;
            } catch (Throwable t) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "MySQL storage failed to initialise; falling back to YAML.", t);
            }
        }
        return new YamlDataStore(plugin);
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

    public ChatPrompts chatPrompts() {
        return chatPrompts;
    }
}
