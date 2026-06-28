package com.nocrates.animation;

import com.nocrates.NoCrates;
import com.nocrates.compat.VersionCompat;
import com.nocrates.reward.Reward;
import com.nocrates.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** A whole-grid "wheel" shuffle that decelerates and locks onto the centre. */
public final class RouletteAnimation implements Animation {

    private static final int CENTRE = 13;

    private final NoCrates plugin;

    public RouletteAnimation(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "roulette";
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
        spin(session, menu.getInventory(), pool, won, 0, 28);
    }

    private void spin(CrateSession session, Inventory inventory, List<Reward> pool, Reward won, int frame, int total) {
        Player player = session.player();
        if (!Animations.isViewing(player, inventory)) {
            session.finish();
            return;
        }
        if (frame >= total) {
            ItemStack filler = Items.icon(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int slot = 0; slot < 27; slot++) {
                inventory.setItem(slot, filler);
            }
            inventory.setItem(CENTRE, Animations.glow(won));
            VersionCompat.playSound(player, "entity.player.levelup", 1f, 1f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory();
                session.finish();
            }, 40L);
            return;
        }
        boolean lockCentre = frame > total - 6;
        for (int slot = 0; slot < 27; slot++) {
            if (slot == CENTRE && lockCentre) {
                inventory.setItem(slot, Animations.glow(won));
                continue;
            }
            inventory.setItem(slot, Animations.icon(pool.get(Math.floorMod(frame * 7 + slot, pool.size()))));
        }
        VersionCompat.playSound(player, "block.note_block.hat", 0.5f, 0.8f + frame * 0.04f);
        long delay = 1L + Math.round(5.0 * frame / total);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spin(session, inventory, pool, won, frame + 1, total), delay);
    }
}
