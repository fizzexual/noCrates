package com.nocrates.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistence backend. Implementations: YAML (default), SQLite, MySQL/MariaDB. */
public interface DataStore extends AutoCloseable {

    CompletableFuture<PlayerData> load(UUID id);

    void saveAsync(PlayerData data);

    void saveSync(PlayerData data);

    /** Global (cross-player) win counts per reward for one crate: rewardId -> count. */
    CompletableFuture<Map<String, Integer>> globalWins(String crateId);

    void setGlobalWin(String crateId, String rewardId, int count, long cooldownUntilEpochSec);

    /** rewardId -> cooldown-until epoch seconds. */
    CompletableFuture<Map<String, Long>> globalWinCooldowns(String crateId);

    /** Rolling winner history rows, newest first. */
    CompletableFuture<List<String>> lastWinners(String crateId, int limit);

    void pushWinner(String crateId, String row, int keep);

    void resetGlobalWins(String crateId);

    @Override
    void close();
}
