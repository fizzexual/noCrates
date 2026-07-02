package com.nocrates.modules.chesthunt;

import com.nocrates.animation.OpeningContext;
import com.nocrates.compat.Compat;
import com.nocrates.compat.Scheduling;
import com.nocrates.core.Services;
import com.nocrates.crate.Loc;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RewardGrant;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One running chest hunt: spawned chest positions with their original block snapshots,
 * pick tracking, timeout, and guaranteed restoration.
 */
final class ChestHuntSession {

    private final ChestHuntModule module;
    private final OpeningContext ctx;
    private final int grid;
    private final int chests;
    private final int picks;
    private final int timeoutSeconds;
    private final Map<String, BlockState> snapshots = new HashMap<>();
    private final AtomicBoolean ended = new AtomicBoolean(false);
    private int picked;

    ChestHuntSession(ChestHuntModule module, OpeningContext ctx, int grid, int chests, int picks, int timeoutSeconds) {
        this.module = module;
        this.ctx = ctx;
        this.grid = grid;
        this.chests = chests;
        this.picks = picks;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Places the chests; false when no valid spots exist around the player. */
    boolean start() {
        Player player = ctx.player();
        Location center = player.getLocation().getBlock().getLocation();
        List<Location> candidates = new ArrayList<>();
        int half = grid / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (dx == 0 && dz == 0) continue;
                Location at = center.clone().add(dx, 0, dz);
                // needs an empty spot with something to stand it on
                if (at.getBlock().getType().isAir()
                        && !at.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                    candidates.add(at);
                }
            }
        }
        if (candidates.isEmpty()) return false;
        Collections.shuffle(candidates);
        int count = Math.min(chests, candidates.size());
        for (int i = 0; i < count; i++) {
            Block block = candidates.get(i).getBlock();
            snapshots.put(Loc.key(block), block.getState());
            block.setType(Material.CHEST, false);
            module.registerChest(Loc.key(block), this);
            Compat.spawn(Compat.particle("POOF"), block.getLocation().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0.02);
        }
        Compat.play(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
        Services.get().lang().send(player, "chesthunt-start",
                Placeholder.unparsed("picks", String.valueOf(Math.min(picks, count))),
                Placeholder.unparsed("chests", String.valueOf(count)));
        Scheduling.later(ctx.plugin(), player.getLocation(), timeoutSeconds * 20L, () -> end(false));
        return true;
    }

    void pick(Player player, Block block) {
        if (ended.get() || !player.getUniqueId().equals(ctx.player().getUniqueId())) return;
        String key = Loc.key(block);
        BlockState snapshot = snapshots.remove(key);
        if (snapshot == null) return;
        picked++;
        restore(snapshot);
        Compat.playAt(block.getLocation(), "BLOCK_CHEST_OPEN", 1f, 1f);
        Compat.spawn(Compat.particle("FIREWORK"), block.getLocation().add(0.5, 0.8, 0.5), 12, 0.2, 0.2, 0.2, 0.05);

        Reward reward;
        if (picked == 1) {
            // The first chest reveals the crate's real rolled outcome (granted at end).
            reward = ctx.outcome().get(0);
        } else {
            var services = Services.get();
            // bonus picks are not openings — they must not trip guaranteed-win pity
            var rolled = services.openService().rollOutcome(player, ctx.crate(), 1, false);
            reward = rolled == null ? null : rolled.rewards().get(0);
            if (reward != null) {
                boolean alternative = rolled.alternative()[0];
                if (!alternative) {
                    services.winLimits().record(services.players().of(player), ctx.crate(), reward);
                }
                RewardGrant.grant(player, ctx.crate(), reward, alternative, true);
            }
        }
        Services.get().lang().send(player, "chesthunt-picked",
                Placeholder.parsed("reward", reward == null ? "?" : reward.displayName()),
                Placeholder.unparsed("remaining", String.valueOf(Math.max(0, picks - picked))));
        if (picked >= picks || snapshots.isEmpty()) {
            end(false);
        }
    }

    /** Restores all remaining chests and completes the opening phase. */
    void end(boolean silent) {
        if (!ended.compareAndSet(false, true)) return;
        for (BlockState snapshot : new ArrayList<>(snapshots.values())) {
            restore(snapshot);
        }
        snapshots.clear();
        module.unregisterSession(this);
        if (!silent && ctx.player().isOnline()) {
            Services.get().lang().send(ctx.player(), "chesthunt-over");
        }
        ctx.phaseDone();
    }

    private void restore(BlockState snapshot) {
        Location loc = snapshot.getLocation();
        Scheduling.run(ctx.plugin(), loc, () -> snapshot.update(true, false));
    }
}
