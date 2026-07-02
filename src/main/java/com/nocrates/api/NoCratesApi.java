package com.nocrates.api;

import com.nocrates.action.ActionType;
import com.nocrates.action.Actions;
import com.nocrates.animation.AnimationService;
import com.nocrates.animation.IdleShape;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.crate.PlacementManager;
import com.nocrates.hook.CustomItemProvider;
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
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Everything addons may touch. Obtain via {@link Addon#api()}. */
public interface NoCratesApi {

    Plugin plugin();

    CrateRegistry crates();

    PlacementManager placements();

    KeyRegistry keys();

    KeyService keyService();

    OpenService openService();

    AnimationService animations();

    Actions actions();

    Lang lang();

    MenuConfig menus();

    DataStore storage();

    PlayerCache players();

    StatsService stats();

    WinLimitService winLimits();

    RerollService rerolls();

    default void registerPre(PreOpenAnimation animation) {
        animations().register(animation);
    }

    default void registerPost(PostOpenAnimation animation) {
        animations().register(animation);
    }

    default void registerDisplay(RewardDisplayAnimation animation) {
        animations().register(animation);
    }

    default void registerShape(IdleShape shape) {
        animations().register(shape);
    }

    default void registerAction(ActionType type) {
        actions().register(type);
    }

    /** Registers a %nocrates_<prefix>_...% placeholder resolver. */
    void registerPlaceholder(String prefix, BiFunction<OfflinePlayer, String, String> resolver);

    /** Registers an extra /crates verb, e.g. "claim". */
    default void registerCommand(String verb, BiConsumer<org.bukkit.command.CommandSender, String[]> handler) {
        registerCommand(verb, null, handler, null);
    }

    /**
     * Full form: {@code permission} gates execution and hides the verb from tab
     * suggestions; {@code completer} receives the args after the verb and returns
     * candidates for the argument currently being typed.
     */
    void registerCommand(String verb, String permission,
                         BiConsumer<org.bukkit.command.CommandSender, String[]> handler,
                         BiFunction<org.bukkit.command.CommandSender, String[], java.util.List<String>> completer);

    /** Registers a custom-item namespace ("mynamespace:id" in ItemSpecs). */
    void registerCustomItems(CustomItemProvider provider);
}
