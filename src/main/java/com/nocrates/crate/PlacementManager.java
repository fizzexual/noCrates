package com.nocrates.crate;

import com.nocrates.compat.Scheduling;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every placed crate. Visuals follow chunk load/unload; placements are updated
 * live by attach/detach/place and rebuilt from crate files on reload.
 */
public final class PlacementManager implements Listener {

    private final Plugin plugin;
    private final CrateRegistry crates;
    private final Map<String, CratePlacement> byLocation = new ConcurrentHashMap<>();

    public PlacementManager(Plugin plugin, CrateRegistry crates) {
        this.plugin = plugin;
        this.crates = crates;
    }

    /** Rebuild from crate definitions (enable + /crates reload). */
    public void rebuild() {
        for (CratePlacement placement : byLocation.values()) {
            Location loc = placement.location();
            if (loc != null) Scheduling.run(plugin, loc, placement::despawnVisuals);
        }
        byLocation.clear();
        for (Crate crate : crates.all()) {
            for (String locKey : crate.locations()) {
                CratePlacement placement = new CratePlacement(crate, locKey);
                byLocation.put(locKey, placement);
                Location loc = placement.location();
                if (loc != null) Scheduling.run(plugin, loc, placement::spawnVisuals);
            }
        }
    }

    /** Respawns visuals for ONE crate's placements — editor saves don't flicker the whole server. */
    public void refresh(Crate crate) {
        // drop placements whose location was removed from the crate
        for (CratePlacement placement : new java.util.ArrayList<>(byLocation.values())) {
            if (placement.crate() == crate && !crate.locations().contains(placement.locKey())) {
                byLocation.remove(placement.locKey());
                Location loc = placement.location();
                if (loc != null) Scheduling.run(plugin, loc, placement::despawnVisuals);
            }
        }
        for (String locKey : crate.locations()) {
            CratePlacement placement = byLocation.get(locKey);
            if (placement == null || placement.crate() != crate) {
                placement = new CratePlacement(crate, locKey);
                byLocation.put(locKey, placement);
            }
            CratePlacement current = placement;
            Location loc = current.location();
            if (loc != null) {
                Scheduling.run(plugin, loc, () -> {
                    current.despawnVisuals();
                    current.spawnVisuals();
                });
            }
        }
    }

    public void shutdown() {
        for (CratePlacement placement : byLocation.values()) {
            placement.despawnVisuals();
        }
        byLocation.clear();
    }

    public CratePlacement at(Block block) {
        return byLocation.get(Loc.key(block));
    }

    public CratePlacement at(String locKey) {
        return byLocation.get(locKey);
    }

    public Collection<CratePlacement> all() {
        return byLocation.values();
    }

    public List<CratePlacement> of(Crate crate) {
        List<CratePlacement> out = new ArrayList<>();
        for (CratePlacement placement : byLocation.values()) {
            if (placement.crate() == crate) out.add(placement);
        }
        return out;
    }

    public CratePlacement attach(Crate crate, Block block) {
        String locKey = Loc.key(block);
        crate.locations().add(locKey);
        crates.save(crate);
        CratePlacement placement = new CratePlacement(crate, locKey);
        byLocation.put(locKey, placement);
        placement.spawnVisuals();
        return placement;
    }

    /** Removes the placement bound to this block; returns its crate or null. */
    public Crate detach(Block block) {
        CratePlacement placement = byLocation.remove(Loc.key(block));
        if (placement == null) return null;
        placement.despawnVisuals();
        Crate crate = placement.crate();
        crate.locations().remove(placement.locKey());
        crates.save(crate);
        return crate;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        forChunk(event.getChunk(), placement -> {
            if (!placement.visualsSpawned()) placement.spawnVisuals();
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        forChunk(event.getChunk(), CratePlacement::despawnVisuals);
    }

    private void forChunk(Chunk chunk, java.util.function.Consumer<CratePlacement> fn) {
        String world = chunk.getWorld().getName();
        for (CratePlacement placement : byLocation.values()) {
            if (!Loc.worldOf(placement.locKey()).equals(world)) continue;
            Location loc = Loc.parse(placement.locKey());
            if (loc == null) continue;
            if (loc.getBlockX() >> 4 == chunk.getX() && loc.getBlockZ() >> 4 == chunk.getZ()) {
                fn.accept(placement);
            }
        }
    }

    /** Crate blocks are indestructible; detach them instead. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (at(event.getBlock()) != null) event.setCancelled(true);
    }
}
