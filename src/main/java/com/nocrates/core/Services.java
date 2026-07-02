package com.nocrates.core;

import com.nocrates.NoCratesPlugin;
import com.nocrates.action.Actions;
import com.nocrates.animation.AnimationService;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.crate.PlacementManager;
import com.nocrates.key.KeyRegistry;
import com.nocrates.key.KeyService;
import com.nocrates.logging.ActionLogger;
import com.nocrates.menu.MenuConfig;
import com.nocrates.open.OpenService;
import com.nocrates.reroll.RerollService;
import com.nocrates.reward.WinLimitService;
import com.nocrates.stats.StatsService;
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
    private KeyRegistry keys;
    private KeyService keyService;
    private CrateRegistry crates;
    private PlacementManager placements;
    private AnimationService animations;
    private WinLimitService winLimits;
    private OpenService openService;
    private RerollService rerolls;
    private StatsService stats;

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

    public void menus(MenuConfig v) {
        this.menus = v;
    }

    public Actions actions() {
        return actions;
    }

    public void actions(Actions v) {
        this.actions = v;
    }

    public DataStore dataStore() {
        return dataStore;
    }

    public void dataStore(DataStore v) {
        this.dataStore = v;
    }

    public PlayerCache players() {
        return players;
    }

    public void players(PlayerCache v) {
        this.players = v;
    }

    public ActionLogger actionLogger() {
        return actionLogger;
    }

    public void actionLogger(ActionLogger v) {
        this.actionLogger = v;
    }

    public KeyRegistry keys() {
        return keys;
    }

    public void keys(KeyRegistry v) {
        this.keys = v;
    }

    public KeyService keyService() {
        return keyService;
    }

    public void keyService(KeyService v) {
        this.keyService = v;
    }

    public CrateRegistry crates() {
        return crates;
    }

    public void crates(CrateRegistry v) {
        this.crates = v;
    }

    public PlacementManager placements() {
        return placements;
    }

    public void placements(PlacementManager v) {
        this.placements = v;
    }

    public AnimationService animations() {
        return animations;
    }

    public void animations(AnimationService v) {
        this.animations = v;
    }

    public WinLimitService winLimits() {
        return winLimits;
    }

    public void winLimits(WinLimitService v) {
        this.winLimits = v;
    }

    public OpenService openService() {
        return openService;
    }

    public void openService(OpenService v) {
        this.openService = v;
    }

    public RerollService rerolls() {
        return rerolls;
    }

    public void rerolls(RerollService v) {
        this.rerolls = v;
    }

    public StatsService stats() {
        return stats;
    }

    public void stats(StatsService v) {
        this.stats = v;
    }
}
