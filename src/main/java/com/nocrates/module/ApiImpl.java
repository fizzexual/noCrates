package com.nocrates.module;

import com.nocrates.action.Actions;
import com.nocrates.animation.AnimationService;
import com.nocrates.api.NoCratesApi;
import com.nocrates.command.CratesCommand;
import com.nocrates.core.Services;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.crate.PlacementManager;
import com.nocrates.hook.CustomItemProvider;
import com.nocrates.hook.CustomItems;
import com.nocrates.hook.PapiExpansion;
import com.nocrates.key.KeyRegistry;
import com.nocrates.key.KeyService;
import com.nocrates.menu.MenuConfig;
import com.nocrates.open.OpenService;
import com.nocrates.reroll.RerollService;
import com.nocrates.reward.WinLimitService;
import com.nocrates.stats.StatsService;
import com.nocrates.storage.DataStore;
import com.nocrates.storage.PlayerCache;
import com.nocrates.text.Lang;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** The one real {@link NoCratesApi}, backed by {@link Services}. */
public final class ApiImpl implements NoCratesApi {

    @Override
    public Plugin plugin() {
        return Services.get().plugin();
    }

    @Override
    public CrateRegistry crates() {
        return Services.get().crates();
    }

    @Override
    public PlacementManager placements() {
        return Services.get().placements();
    }

    @Override
    public KeyRegistry keys() {
        return Services.get().keys();
    }

    @Override
    public KeyService keyService() {
        return Services.get().keyService();
    }

    @Override
    public OpenService openService() {
        return Services.get().openService();
    }

    @Override
    public AnimationService animations() {
        return Services.get().animations();
    }

    @Override
    public Actions actions() {
        return Services.get().actions();
    }

    @Override
    public Lang lang() {
        return Services.get().lang();
    }

    @Override
    public MenuConfig menus() {
        return Services.get().menus();
    }

    @Override
    public DataStore storage() {
        return Services.get().dataStore();
    }

    @Override
    public PlayerCache players() {
        return Services.get().players();
    }

    @Override
    public StatsService stats() {
        return Services.get().stats();
    }

    @Override
    public WinLimitService winLimits() {
        return Services.get().winLimits();
    }

    @Override
    public RerollService rerolls() {
        return Services.get().rerolls();
    }

    @Override
    public void registerPlaceholder(String prefix, BiFunction<OfflinePlayer, String, String> resolver) {
        PapiExpansion.register(prefix, resolver);
    }

    @Override
    public void registerCommand(String verb, String permission, BiConsumer<CommandSender, String[]> handler,
                                BiFunction<CommandSender, String[], java.util.List<String>> completer) {
        CratesCommand.registerExtra(verb, permission, handler, completer);
    }

    @Override
    public void registerCustomItems(CustomItemProvider provider) {
        CustomItems.register(provider);
    }
}
