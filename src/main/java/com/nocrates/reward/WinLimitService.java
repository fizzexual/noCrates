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

    /** Pre-warms the global caches; call at startup/reload for every crate. */
    public void warm(String crateId) {
        store.globalWins(crateId).thenAccept(counts ->
                globalCounts.put(crateId, new ConcurrentHashMap<>(counts)));
        store.globalWinCooldowns(crateId).thenAccept(cds ->
                globalCooldowns.put(crateId, new ConcurrentHashMap<>(cds)));
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
        long cooldownUntil = 0;
        WinLimit gl = reward.globalLimit();
        if (gl.cooldownSeconds() > 0 && !gl.unlimited() && count >= gl.max()) {
            cooldownUntil = now + gl.cooldownSeconds();
            counts.put(reward.id(), 0);
            count = 0;
            globalCooldowns.computeIfAbsent(crate.id(), k -> new ConcurrentHashMap<>())
                    .put(reward.id(), cooldownUntil);
        }
        store.setGlobalWin(crate.id(), reward.id(), count, cooldownUntil);
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
