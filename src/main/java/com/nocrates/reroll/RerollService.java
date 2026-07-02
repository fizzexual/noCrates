package com.nocrates.reroll;

import com.nocrates.crate.Crate;
import com.nocrates.storage.PlayerCache;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Reroll sources stack, PhoenixCrates-style: free rerolls per opening + permission
 * groups (nocrates.reroll.&lt;group&gt;, amounts summed) + an admin-granted persistent
 * balance. Temporary (free+group) rerolls are consumed before the granted balance.
 */
public final class RerollService {

    private final PlayerCache players;

    public RerollService(PlayerCache players) {
        this.players = players;
    }

    /** Temporary rerolls available for one opening session. */
    public int sessionAllowance(Player player, Crate crate) {
        if (!crate.rerollEnabled()) return 0;
        int allowance = crate.rerollFree();
        for (Map.Entry<String, Integer> group : crate.rerollGroups().entrySet()) {
            if (player.hasPermission("nocrates.reroll." + group.getKey())) {
                allowance += Math.max(0, group.getValue());
            }
        }
        return allowance;
    }

    public int grantedBalance(Player player, Crate crate) {
        return players.of(player).rerolls(crate.id());
    }

    /** Total rerolls the player could still use given already-used temporary ones. */
    public int available(Player player, Crate crate, int temporaryLeft) {
        if (!crate.rerollEnabled()) return 0;
        return Math.max(0, temporaryLeft) + grantedBalance(player, crate);
    }

    /**
     * Consumes one reroll. Returns the new temporary-left counter, or -1 when nothing
     * was available to consume.
     */
    public int consume(Player player, Crate crate, int temporaryLeft) {
        if (temporaryLeft > 0) return temporaryLeft - 1;
        var data = players.of(player);
        if (data.rerolls(crate.id()) > 0) {
            data.addRerolls(crate.id(), -1);
            return 0;
        }
        return -1;
    }

    public void give(java.util.UUID player, Crate crate, int amount) {
        players.withOffline(player, data -> data.addRerolls(crate.id(), Math.max(0, amount)));
    }

    public void take(java.util.UUID player, Crate crate, int amount) {
        players.withOffline(player, data -> data.addRerolls(crate.id(), -Math.max(0, amount)));
    }
}
