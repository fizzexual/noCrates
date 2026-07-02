package com.nocrates.crate;

import com.nocrates.compat.Compat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One placed crate in the world. Owns its visuals: the block (BLOCK engine) or a
 * floating ItemDisplay model (MODEL engine) plus the hologram, and the per-placement
 * opening lock used when simultaneous openings are disabled.
 */
public final class CratePlacement {

    public static final NamespacedKey PDC_MODEL = NamespacedKey.fromString("nocrates:model");

    private final Crate crate;
    private final String locKey;
    private final Hologram hologram = new Hologram();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private UUID modelEntity;
    private boolean visualsSpawned;

    public CratePlacement(Crate crate, String locKey) {
        this.crate = crate;
        this.locKey = locKey;
    }

    public Crate crate() {
        return crate;
    }

    public String locKey() {
        return locKey;
    }

    /** Null while the world/chunk is unavailable. */
    public Location location() {
        return Loc.parse(locKey);
    }

    public boolean busy() {
        return busy.get();
    }

    /** Attempts to lock the placement for an opening; false when someone else holds it. */
    public boolean lock() {
        return busy.compareAndSet(false, true);
    }

    public void unlock() {
        busy.set(false);
    }

    /** Must run on the region thread that owns this location. */
    public void spawnVisuals() {
        Location loc = location();
        if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        if (crate.engine() == Crate.EngineType.BLOCK) {
            Material material = Compat.material(crate.blockMaterial(), Material.CHEST);
            Block block = loc.getBlock();
            if (block.getType() != material) block.setType(material, false);
        } else {
            spawnModel(loc);
        }
        hologram.spawn(loc, crate.hologramLines(), crate.hologramOffset(), crate.displayName());
        visualsSpawned = true;
    }

    private void spawnModel(Location loc) {
        removeModel(loc);
        Location at = loc.clone().add(0.5, 0.5 + crate.modelYOffset(), 0.5);
        cleanupStaleModels(at);
        ItemDisplay display = loc.getWorld().spawn(at, ItemDisplay.class);
        display.setItemStack(crate.modelItem().build());
        display.setBillboard(Display.Billboard.FIXED);
        display.setPersistent(false);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f((float) Math.toRadians(crate.modelYaw()), 0, 1, 0),
                new Vector3f(1f, 1f, 1f),
                new AxisAngle4f(0, 0, 1, 0)));
        display.getPersistentDataContainer().set(PDC_MODEL, PersistentDataType.STRING, locKey);
        modelEntity = display.getUniqueId();
    }

    public void despawnVisuals() {
        Location loc = location();
        if (loc == null) return;
        hologram.remove(loc.getWorld());
        removeModel(loc);
        visualsSpawned = false;
    }

    public boolean visualsSpawned() {
        return visualsSpawned;
    }

    private void removeModel(Location loc) {
        if (modelEntity == null) return;
        var entity = loc.getWorld().getEntity(modelEntity);
        if (entity != null) entity.remove();
        modelEntity = null;
    }

    private void cleanupStaleModels(Location at) {
        for (var entity : at.getWorld().getNearbyEntities(at, 0.6, 1.2, 0.6)) {
            if (entity instanceof ItemDisplay
                    && entity.getPersistentDataContainer().has(PDC_MODEL, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    /** Plays the native lid animation on Lidded blocks (chests, barrels, shulkers). */
    public void setLidOpen(boolean open) {
        Location loc = location();
        if (loc == null || crate.engine() != Crate.EngineType.BLOCK) return;
        BlockState state = loc.getBlock().getState();
        if (state instanceof Lidded lidded) {
            if (open) lidded.open();
            else lidded.close();
        }
    }

    /** The point animations orbit around: block center, slightly above. */
    public Location effectAnchor() {
        Location loc = location();
        return loc == null ? null : loc.clone().add(0.5, 1.1, 0.5);
    }
}
