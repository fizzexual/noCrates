package com.nocrates.modules.massopen;

import com.nocrates.api.Addon;
import com.nocrates.crate.Crate;
import com.nocrates.crate.RewardsMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Mass Opening module: open many keys at once through tier
 * buttons (x1/x6/x12/x32/x64/all), gated by nocrates.massopen.&lt;tier&gt; permissions,
 * with an aggregated summary instead of reward spam.
 */
public final class MassOpenModule extends Addon {

    private final List<Integer> tiers = new ArrayList<>();
    private boolean allowAll = true;
    private int allCap = 100;

    @Override
    public void onEnable() {
        for (int tier : config().getIntegerList("tiers")) {
            if (tier > 0) tiers.add(tier);
        }
        if (tiers.isEmpty()) tiers.addAll(List.of(1, 6, 12, 32, 64));
        allowAll = config().getBoolean("allow-all", true);
        allCap = Math.max(1, config().getInt("all-cap", 100));

        api().registerCommand("massopen", null, (sender, args) -> {
            if (!(sender instanceof Player player)) {
                api().lang().send(sender, "player-only");
                return;
            }
            if (args.length == 0) {
                api().lang().send(sender, "unknown-command");
                return;
            }
            Crate crate = api().crates().get(args[0]);
            if (crate == null) {
                api().lang().send(sender, "crate-not-found",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("crate", args[0]));
                return;
            }
            if (crate.rewardsMode() == RewardsMode.SELECTIVE || crate.keys().isEmpty()) {
                api().lang().send(sender, "module-disabled");
                return;
            }
            new MassOpenMenu(player, crate, this).open();
        }, (sender, args) -> args.length == 1
                ? new java.util.ArrayList<>(api().crates().ids()) : List.of());
    }

    public List<Integer> tiers() {
        return tiers;
    }

    public boolean allowAll() {
        return allowAll;
    }

    public int allCap() {
        return allCap;
    }
}
