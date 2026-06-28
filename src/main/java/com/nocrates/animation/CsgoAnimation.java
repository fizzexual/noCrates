package com.nocrates.animation;

import com.nocrates.NoCrates;
import com.nocrates.compat.VersionCompat;
import com.nocrates.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * CS:GO-style horizontal spinner: a row of reward icons scrolls and decelerates
 * (via a self-rescheduling step with growing delay), landing the won reward
 * under the centre marker.
 */
public final class CsgoAnimation implements Animation {

    private static final int[] ROW = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int CENTRE = 13;

    private final NoCrates plugin;

    public CsgoAnimation(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "csgo";
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
        ItemStack marker = com.nocrates.util.Items.icon(Material.HOPPER, "<gray>▼");
        inventory.setItem(4, marker);
        inventory.setItem(22, marker);

        step(session, inventory, pool, won, 0, 30);
    }

    private void step(CrateSession session, Inventory inventory, List<Reward> pool, Reward won, int frame, int total) {
        Player player = session.player();
        if (!Animations.isViewing(player, inventory)) {
            session.finish();
            return;
        }
        for (int i = 0; i < ROW.length; i++) {
            inventory.setItem(ROW[i], Animations.icon(pool.get(Math.floorMod(frame + i, pool.size()))));
        }
        if (frame >= total) {
            inventory.setItem(CENTRE, Animations.glow(won));
            VersionCompat.playSound(player, "entity.player.levelup", 1f, 1f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory();
                session.finish();
            }, 40L);
            return;
        }
        VersionCompat.playSound(player, "ui.button.click", 0.4f, 1.0f + frame * 0.03f);
        long delay = 1L + Math.round(6.0 * frame / total);
        Bukkit.getScheduler().runTaskLater(plugin, () -> step(session, inventory, pool, won, frame + 1, total), delay);
    }
}
