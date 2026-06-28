package com.nocrates.chesthunt;

import com.nocrates.NoCrates;
import com.nocrates.compat.VersionCompat;
import com.nocrates.crate.ChestHuntSettings;
import com.nocrates.crate.Crate;
import com.nocrates.message.Messages;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RewardActions;
import com.nocrates.reward.RollEngine;
import com.nocrates.storage.PlayerData;
import com.nocrates.util.Items;
import com.nocrates.util.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The "chest hunt" mechanic: opening spawns a grid of temporary chests around
 * the player who may open only a limited number; each opened chest grants an
 * independently rolled reward. Original blocks are snapshotted and restored, and
 * the chests are protected from breaking (anti-grief).
 */
public final class ChestHuntManager implements Listener {

    private final NoCrates plugin;
    private final RollEngine roll = new RollEngine(new Random());
    private final Map<UUID, ActiveHunt> hunts = new HashMap<>();
    private final Map<String, ActiveHunt> chestIndex = new HashMap<>();

    public ChestHuntManager(NoCrates plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean isHunting(UUID id) {
        return hunts.containsKey(id);
    }

    /** Place the chest grid and start a hunt. Returns false if there's no room. */
    public boolean begin(Player player, Crate crate) {
        if (hunts.containsKey(player.getUniqueId())) {
            return false;
        }
        ChestHuntSettings settings = crate.chestHunt();
        List<Block> spots = findSpots(player, settings);
        if (spots.isEmpty()) {
            return false;
        }
        Collections.shuffle(spots);
        int chestCount = Math.min(settings.chests(), spots.size());
        ActiveHunt hunt = new ActiveHunt(player.getUniqueId(), crate, Math.min(settings.picks(), chestCount));
        for (int i = 0; i < chestCount; i++) {
            Block block = spots.get(i);
            String key = Locations.serialize(block.getLocation());
            hunt.originals.put(key, block.getBlockData());
            block.setType(Material.CHEST);
            chestIndex.put(key, hunt);
        }
        hunt.remaining.addAll(hunt.originals.keySet());
        hunts.put(player.getUniqueId(), hunt);
        hunt.taskId = plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> end(player.getUniqueId(), true), settings.timeoutSeconds() * 20L)
                .getTaskId();
        plugin.services().messages().send(player, "chesthunt-start",
                Messages.ph("picks", String.valueOf(hunt.picksRemaining)),
                Messages.ph("chests", String.valueOf(chestCount)));
        return true;
    }

    private List<Block> findSpots(Player player, ChestHuntSettings settings) {
        List<Block> spots = new ArrayList<>();
        Location base = player.getLocation();
        World world = base.getWorld();
        for (int dx = -settings.radius(); dx <= settings.radius(); dx++) {
            for (int dz = -settings.radius(); dz <= settings.radius(); dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Block block = world.getBlockAt(base.getBlockX() + dx, base.getBlockY(), base.getBlockZ() + dz);
                Block below = block.getRelative(0, -1, 0);
                String key = Locations.serialize(block.getLocation());
                if (isReplaceable(block) && below.getType().isSolid() && !chestIndex.containsKey(key)) {
                    spots.add(block);
                }
            }
        }
        return spots;
    }

    /** Version-safe replaceable check using runtime enum names (avoids renamed constants). */
    private boolean isReplaceable(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return true;
        }
        String name = type.name();
        return name.equals("GRASS") || name.equals("SHORT_GRASS") || name.equals("TALL_GRASS")
                || name.equals("FERN") || name.equals("LARGE_FERN") || name.equals("SNOW")
                || name.equals("DEAD_BUSH");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }
        String key = Locations.serialize(event.getClickedBlock().getLocation());
        ActiveHunt hunt = chestIndex.get(key);
        if (hunt == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || !event.getPlayer().getUniqueId().equals(hunt.owner)) {
            return;
        }
        pick(hunt, key, event.getClickedBlock().getLocation());
    }

    private void pick(ActiveHunt hunt, String key, Location location) {
        Player player = Bukkit.getPlayer(hunt.owner);
        Crate crate = hunt.crate;
        restore(hunt, key);
        chestIndex.remove(key);
        hunt.remaining.remove(key);

        Reward won = roll.roll(crate.rewards());
        if (won != null && player != null) {
            for (String line : won.actions()) {
                RewardActions.parse(line).execute(player);
            }
            PlayerData data = plugin.services().playerData().get(player.getUniqueId());
            data.incrOpens(crate.name());
            data.incrWin(crate.name(), won.id());
            effect(location);
            String raw = (won.display() != null && won.display().name() != null)
                    ? won.display().name() : won.id();
            plugin.services().messages().send(player, "reward-won",
                    Messages.phComponent("reward", Items.mini(raw)));
        }

        hunt.picksRemaining--;
        if (hunt.picksRemaining <= 0 || hunt.remaining.isEmpty()) {
            end(hunt.owner, true);
        }
    }

    private void effect(Location location) {
        VersionCompat.playSound(location, "entity.player.levelup", 1f, 1.2f);
        Particle particle = VersionCompat.particle("HAPPY_VILLAGER");
        if (particle != null && location.getWorld() != null) {
            location.getWorld().spawnParticle(particle, location.clone().add(0.5, 1, 0.5), 20, 0.3, 0.3, 0.3, 0);
        }
    }

    public void end(UUID id, boolean restore) {
        ActiveHunt hunt = hunts.remove(id);
        if (hunt == null) {
            return;
        }
        if (hunt.taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(hunt.taskId);
        }
        for (String key : new ArrayList<>(hunt.remaining)) {
            if (restore) {
                restore(hunt, key);
            }
            chestIndex.remove(key);
        }
        hunt.remaining.clear();
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            plugin.services().messages().send(player, "chesthunt-end");
        }
    }

    private void restore(ActiveHunt hunt, String key) {
        BlockData data = hunt.originals.get(key);
        Location location = Locations.parse(key);
        if (data != null && location != null && location.getWorld() != null) {
            location.getBlock().setBlockData(data, false);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (chestIndex.containsKey(Locations.serialize(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer().getUniqueId(), true);
    }

    public void shutdown() {
        for (UUID id : new ArrayList<>(hunts.keySet())) {
            end(id, true);
        }
    }

    private static final class ActiveHunt {
        final UUID owner;
        final Crate crate;
        final Map<String, BlockData> originals = new HashMap<>();
        final Set<String> remaining = new HashSet<>();
        int picksRemaining;
        int taskId = -1;

        ActiveHunt(UUID owner, Crate crate, int picks) {
            this.owner = owner;
            this.crate = crate;
            this.picksRemaining = picks;
        }
    }
}
