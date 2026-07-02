package com.nocrates.modules.chesthunt;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.api.Addon;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chest Hunt — noCrates' signature mechanic (not in PhoenixCrates): set a crate's
 * reward-display to CHEST_HUNT and opening it spawns M temporary chests around the
 * player; they physically open up to K, each granting an independently rolled reward,
 * then the area restores itself.
 */
public final class ChestHuntModule extends Addon implements Listener, RewardDisplayAnimation {

    private final Map<String, ChestHuntSession> byChest = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, ChestHuntSession> byPlayer = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return "CHEST_HUNT";
    }

    @Override
    public void onEnable() {
        api().registerDisplay(this);
        Bukkit.getPluginManager().registerEvents(this, api().plugin());
    }

    @Override
    public void onDisable() {
        for (ChestHuntSession session : byPlayer.values()) session.end(true);
        byPlayer.clear();
        byChest.clear();
    }

    @Override
    public void play(OpeningContext ctx) {
        ChestHuntSession existing = byPlayer.get(ctx.player().getUniqueId());
        if (existing != null) existing.end(true);
        ChestHuntSession session = new ChestHuntSession(this, ctx,
                Math.max(3, config().getInt("grid", 5)),
                Math.max(1, config().getInt("chests", 8)),
                Math.max(1, config().getInt("picks", 4)),
                Math.max(5, config().getInt("timeout-seconds", 30)));
        if (!session.start()) {
            ctx.phaseDone(); // nowhere to spawn chests — fall back to instant reveal
            return;
        }
        byPlayer.put(ctx.player().getUniqueId(), session);
    }

    void registerChest(String locKey, ChestHuntSession session) {
        byChest.put(locKey, session);
    }

    void unregisterSession(ChestHuntSession session) {
        byChest.values().removeIf(s -> s == session);
        byPlayer.values().removeIf(s -> s == session);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        ChestHuntSession session = byChest.get(com.nocrates.crate.Loc.key(block));
        if (session == null) return;
        event.setCancelled(true);
        session.pick(event.getPlayer(), block);
    }
}
