package com.nocrates.crate;

import com.nocrates.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Built-in hologram: one TextDisplay entity (stable API since 1.19.4, no hologram
 * plugin needed). Entities are tagged so stale ones from a crash are cleaned up.
 */
public final class Hologram {

    public static final NamespacedKey PDC_TAG = NamespacedKey.fromString("nocrates:hologram");

    private UUID entityId;

    /** Spawns (or replaces) the hologram above the block location. */
    public void spawn(Location blockLoc, List<String> lines, double offset, String crateName) {
        remove(blockLoc.getWorld());
        if (lines.isEmpty()) return;
        Location at = blockLoc.clone().add(0.5, offset, 0.5);
        cleanupStale(at);
        TextDisplay display = blockLoc.getWorld().spawn(at, TextDisplay.class);
        display.text(joined(lines, crateName));
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        display.setShadowed(false);
        display.setDefaultBackground(false);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        display.getPersistentDataContainer().set(PDC_TAG, PersistentDataType.STRING, Loc.key(blockLoc));
        entityId = display.getUniqueId();
    }

    public void update(org.bukkit.World world, List<String> lines, String crateName) {
        if (entityId == null) return;
        if (world.getEntity(entityId) instanceof TextDisplay display) {
            display.text(joined(lines, crateName));
        }
    }

    public void remove(org.bukkit.World world) {
        if (entityId == null) return;
        var entity = world.getEntity(entityId);
        if (entity != null) entity.remove();
        entityId = null;
    }

    private static Component joined(List<String> lines, String crateName) {
        Component out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out = out.append(Component.newline());
            out = out.append(Text.mm(lines.get(i).replace("%name%", crateName)));
        }
        return out;
    }

    /** Removes leftover tagged displays near the spot (e.g. after a hard crash). */
    private static void cleanupStale(Location at) {
        for (var entity : at.getWorld().getNearbyEntities(at, 0.6, 2.0, 0.6)) {
            if (entity instanceof TextDisplay
                    && entity.getPersistentDataContainer().has(PDC_TAG, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }
}
