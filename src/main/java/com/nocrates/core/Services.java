package com.nocrates.core;

import com.nocrates.NoCratesPlugin;
import com.nocrates.action.Actions;
import com.nocrates.logging.ActionLogger;
import com.nocrates.menu.MenuConfig;
import com.nocrates.storage.DataStore;
import com.nocrates.storage.PlayerCache;
import com.nocrates.text.Lang;

/** Central service locator, wired once in {@link NoCratesPlugin#onEnable()}. */
public final class Services {

    private static Services instance;

    private final NoCratesPlugin plugin;
    private final MainConfig config;
    private final Lang lang;
    private final ReloadManager reloads = new ReloadManager();

    private MenuConfig menus;
    private Actions actions;
    private DataStore dataStore;
    private PlayerCache players;
    private ActionLogger actionLogger;

    public Services(NoCratesPlugin plugin, MainConfig config, Lang lang) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        instance = this;
    }

    public static Services get() {
        return instance;
    }

    public NoCratesPlugin plugin() {
        return plugin;
    }

    public MainConfig config() {
        return config;
    }

    public Lang lang() {
        return lang;
    }

    public ReloadManager reloads() {
        return reloads;
    }

    public MenuConfig menus() {
        return menus;
    }

    public void menus(MenuConfig menus) {
        this.menus = menus;
    }

    public Actions actions() {
        return actions;
    }

    public void actions(Actions actions) {
        this.actions = actions;
    }

    public DataStore dataStore() {
        return dataStore;
    }

    public void dataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public PlayerCache players() {
        return players;
    }

    public void players(PlayerCache players) {
        this.players = players;
    }

    public ActionLogger actionLogger() {
        return actionLogger;
    }

    public void actionLogger(ActionLogger actionLogger) {
        this.actionLogger = actionLogger;
    }
}
