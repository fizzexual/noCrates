package com.nocrates.animation;

import com.nocrates.NoCrates;
import com.nocrates.compat.VersionCompat;
import com.nocrates.reward.Reward;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * In-world reveal: the won reward floats up above the player as a glowing
 * {@link ItemDisplay} with a firework, then is granted. No GUI.
 */
public final class PhysicalAnimation implements Animation {

    private final NoCrates plugin;

    public PhysicalAnimation(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "physical";
    }

    @Override
    public void play(CrateSession session) {
        Player player = session.player();
        Reward won = session.primary();
        if (won == null) {
            session.finish();
            return;
        }
        Location base = player.getLocation().add(0, 1.9, 0);
        ItemStack icon = Animations.glow(won);
        ItemDisplay display;
        try {
            display = player.getWorld().spawn(base, ItemDisplay.class, entity -> {
                entity.setItemStack(icon);
                entity.setBillboard(Display.Billboard.VERTICAL);
                try {
                    entity.setPersistent(false);
                    entity.setGlowing(true);
                } catch (Throwable ignored) {
                    // optional flair
                }
            });
        } catch (Throwable t) {
            session.finish();
            return;
        }
        VersionCompat.playSound(player, "entity.player.levelup", 1f, 1f);
        spawnFirework(base);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 40 || display.isDead()) {
                    if (!display.isDead()) {
                        display.remove();
                    }
                    cancel();
                    session.finish();
                    return;
                }
                display.teleport(display.getLocation().add(0, 0.02, 0));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void spawnFirework(Location location) {
        try {
            Firework firework = location.getWorld().spawn(location, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.FUCHSIA, Color.AQUA)
                    .with(FireworkEffect.Type.BURST)
                    .flicker(true)
                    .build());
            meta.setPower(0);
            firework.setFireworkMeta(meta);
            firework.detonate();
        } catch (Throwable ignored) {
            // cosmetic only
        }
    }
}
