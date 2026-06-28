package com.nocrates.core;

import com.nocrates.NoCrates;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.gui.MenuListener;
import com.nocrates.hook.Hooks;
import com.nocrates.key.KeyManager;
import com.nocrates.message.Messages;
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
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
    }

    /** Re-read configuration files without a restart. */
    public void reload() {
        config.reload();
        messages.reload();
        rarities.load(plugin);
        crates.reload();
    }

    public void shutdown() {
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
}
