package com.nocrates.animation;

import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/** Cosmetic entities must stay cosmetic: animation fireworks never damage anyone. */
public final class AnimationGuards implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework
                && firework.getPersistentDataContainer().has(Displays.PDC_TAG, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    /** Removes every leftover animation entity (displays, prop mobs) in loaded chunks. */
    public static void sweepLeftovers() {
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            try {
                for (var entity : world.getEntities()) {
                    if (entity.getPersistentDataContainer().has(Displays.PDC_TAG, PersistentDataType.STRING)) {
                        entity.remove();
                    }
                }
            } catch (Exception ignored) {
                // Folia: cross-region iteration may fail during shutdown; chunk unload
                // still discards these non-persistent entities.
            }
        }
    }
}
