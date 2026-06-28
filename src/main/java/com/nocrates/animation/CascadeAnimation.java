package com.nocrates.animation;

import com.nocrates.NoCrates;
import com.nocrates.compat.VersionCompat;
import com.nocrates.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/** Reward icons cascade row by row, then settle on the centre slot. */
public final class CascadeAnimation implements Animation {

    private static final int CENTRE = 13;

    private final NoCrates plugin;

    public CascadeAnimation(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "cascade";
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
            int frame = 0;
            final int total = 33;

            @Override
            public void run() {
                if (!Animations.isViewing(player, inventory)) {
                    cancel();
                    session.finish();
                    return;
                }
                if (frame >= total) {
                    inventory.setItem(CENTRE, Animations.glow(won));
                    VersionCompat.playSound(player, "entity.player.levelup", 1f, 1f);
                    cancel();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.closeInventory();
                        session.finish();
                    }, 35L);
                    return;
                }
                int row = (frame / 2) % 3;
                for (int col = 0; col < 9; col++) {
                    int slot = row * 9 + col;
                    inventory.setItem(slot, Animations.icon(pool.get(Math.floorMod(frame + slot, pool.size()))));
                }
                VersionCompat.playSound(player, "block.note_block.pling", 0.4f, 0.7f + frame * 0.03f);
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
