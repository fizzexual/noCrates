package com.nocrates.block;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CrateBlock;
import com.nocrates.open.PreviewMenu;
import com.nocrates.util.Locations;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders crate-block holograms (spawning/despawning with their chunks) and
 * turns right-clicks into opens and left-clicks into previews.
 */
public final class CrateBlockManager implements Listener {

    private final NoCrates plugin;
    private final Map<String, Hologram> holograms = new HashMap<>();

    public CrateBlockManager(NoCrates plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        spawnAllLoaded();
    }

    public void refresh() {
        removeAll();
        spawnAllLoaded();
    }

    public void shutdown() {
        removeAll();
    }

    private void spawnAllLoaded() {
        for (Crate crate : plugin.services().crates().all()) {
            CrateBlock block = crate.block();
            if (block == null || !block.isEnabled()) {
                continue;
            }
            for (String key : block.locations()) {
                Location location = Locations.parse(key);
                if (location != null && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    spawn(crate, location, key);
                }
            }
        }
    }

    private void spawn(Crate crate, Location location, String key) {
        if (holograms.containsKey(key) || crate.block().hologram().isEmpty()) {
            return;
        }
        Hologram hologram = new Hologram();
        hologram.spawn(location, crate.block().hologram());
        holograms.put(key, hologram);
    }

    private void removeAll() {
        holograms.values().forEach(Hologram::remove);
        holograms.clear();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (Crate crate : plugin.services().crates().all()) {
            CrateBlock block = crate.block();
            if (block == null || !block.isEnabled()) {
                continue;
            }
            for (String key : block.locations()) {
                Location location = Locations.parse(key);
                if (location != null && location.getWorld().equals(chunk.getWorld())
                        && (location.getBlockX() >> 4) == chunk.getX()
                        && (location.getBlockZ() >> 4) == chunk.getZ()) {
                    spawn(crate, location, key);
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        holograms.entrySet().removeIf(entry -> {
            Location location = Locations.parse(entry.getKey());
            if (location != null && location.getWorld().equals(chunk.getWorld())
                    && (location.getBlockX() >> 4) == chunk.getX()
                    && (location.getBlockZ() >> 4) == chunk.getZ()) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        Crate crate = findCrateAt(block.getLocation());
        if (crate == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            plugin.services().openController().open(player, crate);
        } else {
            new PreviewMenu(crate, plugin.services().rarities()).open(player);
        }
    }

    private Crate findCrateAt(Location location) {
        for (Crate crate : plugin.services().crates().all()) {
            CrateBlock block = crate.block();
            if (block == null || !block.isEnabled()) {
                continue;
            }
            for (String key : block.locations()) {
                if (Locations.sameBlock(location, Locations.parse(key))) {
                    return crate;
                }
            }
        }
        return null;
    }
}
