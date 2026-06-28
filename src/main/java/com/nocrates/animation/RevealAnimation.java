package com.nocrates.animation;

import com.nocrates.NoCrates;
import com.nocrates.compat.VersionCompat;
import com.nocrates.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/** Cycles reward icons in the centre slot, then settles on the won reward. */
public final class RevealAnimation implements Animation {

    private static final int CENTRE = 13;

    private final NoCrates plugin;

    public RevealAnimation(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "reveal";
    }

    @Override
    public void play(CrateSession session) {
        Player player = session.player();
        List<Reward> pool = session.crate().rewards();
        Reward won = session.primary();
        if (pool.isEmpty() || won == null) {
            session.finish();
            return;
        }
        SpinnerMenu menu = new SpinnerMenu(session.crate());
        menu.open(player);
        Inventory inventory = menu.getInventory();

        new BukkitRunnable() {
            int ticks = 0;
            final int total = 40;

            @Override
            public void run() {
                if (!Animations.isViewing(player, inventory)) {
                    cancel();
                    session.finish();
                    return;
                }
                if (ticks < total) {
                    Reward shown = pool.get(ticks % pool.size());
                    inventory.setItem(CENTRE, Animations.icon(shown));
                    VersionCompat.playSound(player, "ui.button.click", 0.4f, 1.0f + (ticks * 0.02f));
                    ticks++;
                } else {
                    inventory.setItem(CENTRE, Animations.glow(won));
                    VersionCompat.playSound(player, "entity.player.levelup", 1f, 1f);
                    cancel();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.closeInventory();
                        session.finish();
                    }, 35L);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
