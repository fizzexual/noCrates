package com.nocrates.stats;

import com.nocrates.storage.PlayerCache;
import org.bukkit.entity.Player;

/** Convenience view over player stats for placeholders and commands. */
public final class StatsService {

    private final PlayerCache players;

    public StatsService(PlayerCache players) {
        this.players = players;
    }

    public int opens(Player player, String crateId) {
        return players.of(player).opens(crateId);
    }

    public int totalOpens(Player player) {
        return players.of(player).totalOpens();
    }

    public long cooldownRemaining(Player player, String crateId) {
        long until = players.of(player).cooldownUntil(crateId);
        return Math.max(0, until - java.time.Instant.now().getEpochSecond());
    }
}
