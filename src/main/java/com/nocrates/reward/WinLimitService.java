package com.nocrates.reward;

import com.nocrates.crate.Crate;
import com.nocrates.storage.DataStore;
import com.nocrates.storage.PlayerData;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Win-limit gate. Player limits read live player data; global limits work against an
 * in-memory cache (loaded lazily per crate, optimistically incremented, persisted
 * asynchronously) so the open pipeline never blocks on the database.
 */
public final class WinLimitService {

    private final DataStore store;
    private final Map<String, Map<String, Integer>> globalCounts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> globalCooldowns = new ConcurrentHashMap<>();

    public WinLimitService(DataStore store) {
        this.store = store;
    }

    /**
     * Pre-warms the global caches; call at startup/reload for every crate. Values are
     * MAX-merged into whatever the cache accumulated meanwhile so wins recorded during
     * the async window are never discarded.
     */
    public void warm(String crateId) {
        store.globalWins(crateId).thenAccept(counts -> {
            Map<String, Integer> cached = globalCounts.computeIfAbsent(crateId, k -> new ConcurrentHashMap<>());
            counts.forEach((reward, dbCount) -> cached.merge(reward, dbCount, Math::max));
        });
        store.globalWinCooldowns(crateId).thenAccept(cds -> {
            Map<String, Long> cached = globalCooldowns.computeIfAbsent(crateId, k -> new ConcurrentHashMap<>());
            cds.forEach((reward, until) -> cached.merge(reward, until, Math::max));
        });
    }

    public boolean allows(PlayerData player, Crate crate, Reward reward) {
        long now = Instant.now().getEpochSecond();
        if (!reward.playerLimit().allows(
                player.wins(crate.id(), reward.id()),
                player.winCooldownUntil(crate.id(), reward.id()), now)) {
            return false;
        }
        int globalCount = globalCounts
                .computeIfAbsent(crate.id(), k -> new ConcurrentHashMap<>())
                .getOrDefault(reward.id(), 0);
        long globalCooldown = globalCooldowns
                .computeIfAbsent(crate.id(), k -> new ConcurrentHashMap<>())
                .getOrDefault(reward.id(), 0L);
        return reward.globalLimit().allows(globalCount, globalCooldown, now);
    }

    /** Records a win everywhere it counts; applies cooldowns when limits are reached. */
    public void record(PlayerData player, Crate crate, Reward reward) {
        long now = Instant.now().getEpochSecond();
        player.incrWins(crate.id(), reward.id());
        WinLimit pl = reward.playerLimit();
        if (pl.cooldownSeconds() > 0 && !pl.unlimited()
                && player.wins(crate.id(), reward.id()) >= pl.max()) {
            // Limit reached: reset the counter and start the cooldown window.
            player.resetWins(crate.id(), reward.id());
            player.setWinCooldown(crate.id(), reward.id(), now + pl.cooldownSeconds());
        }
        Map<String, Integer> counts = globalCounts.computeIfAbsent(crate.id(), k -> new ConcurrentHashMap<>());
        int count = counts.merge(reward.id(), 1, Integer::sum);
        WinLimit gl = reward.globalLimit();
        if (gl.cooldownSeconds() > 0 && !gl.unlimited() && count >= gl.max()) {
            long cooldownUntil = now + gl.cooldownSeconds();
            counts.put(reward.id(), 0);
            globalCooldowns.computeIfAbsent(crate.id(), k -> new ConcurrentHashMap<>())
                    .put(reward.id(), cooldownUntil);
            // absolute write is intended here: the window closed, counter restarts
            store.setGlobalWin(crate.id(), reward.id(), 0, cooldownUntil);
        } else {
            // delta write: never overwrites counts recorded elsewhere (warm race, other servers)
            store.incrGlobalWin(crate.id(), reward.id());
        }
    }

    public void resetGlobal(String crateId) {
        globalCounts.remove(crateId);
        globalCooldowns.remove(crateId);
        store.resetGlobalWins(crateId);
    }

    public int globalCount(String crateId, String rewardId) {
        return globalCounts.getOrDefault(crateId, Map.of()).getOrDefault(rewardId, 0);
    }
}
