package com.nocrates.open;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RewardGrant;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One opening in flight: the pre-rolled outcome plus grant bookkeeping. Granting is
 * idempotent — animations/menus may fail or double-fire without duplicating rewards.
 */
public final class OpenSession {

    private final Player player;
    private final Crate crate;
    private final CratePlacement placement;
    private final boolean quick;
    private final List<Reward> outcome;
    private final boolean[] useAlternative;
    private final AtomicBoolean granted = new AtomicBoolean(false);
    /** Temporary rerolls left in this session (free + permission groups). */
    private int temporaryRerolls;

    public OpenSession(Player player, Crate crate, CratePlacement placement, boolean quick,
                       List<Reward> outcome, boolean[] useAlternative, int temporaryRerolls) {
        this.player = player;
        this.crate = crate;
        this.placement = placement;
        this.quick = quick;
        this.outcome = outcome;
        this.useAlternative = useAlternative;
        this.temporaryRerolls = temporaryRerolls;
    }

    public Player player() {
        return player;
    }

    public Crate crate() {
        return crate;
    }

    public CratePlacement placement() {
        return placement;
    }

    public boolean quick() {
        return quick;
    }

    public List<Reward> outcome() {
        return outcome;
    }

    public boolean alternative(int index) {
        return index < useAlternative.length && useAlternative[index];
    }

    public void replaceOutcome(int index, Reward reward, boolean alternative) {
        outcome.set(index, reward);
        useAlternative[index] = alternative;
    }

    public int temporaryRerolls() {
        return temporaryRerolls;
    }

    public void temporaryRerolls(int temporaryRerolls) {
        this.temporaryRerolls = temporaryRerolls;
    }

    public boolean grantedAlready() {
        return granted.get();
    }

    /** Grants every rolled reward exactly once and releases the placement. */
    public void grantAll() {
        if (!granted.compareAndSet(false, true)) return;
        var services = Services.get();
        if (!player.isOnline()) {
            grantOffline(services);
            release();
            return;
        }
        var winLimits = services.winLimits();
        var data = services.players().of(player);
        for (int i = 0; i < outcome.size(); i++) {
            Reward reward = outcome.get(i);
            if (!alternative(i)) winLimits.record(data, crate, reward);
            RewardGrant.grant(player, crate, reward, alternative(i));
        }
        release();
    }

    /**
     * The player quit mid-animation: never touch their (gone) inventory or the player
     * cache — merge wins into stored data, run console commands, and stash item
     * rewards as claim rows so nothing is lost or wiped.
     */
    private void grantOffline(com.nocrates.core.Services services) {
        java.util.UUID id = player.getUniqueId();
        long now = java.time.Instant.now().getEpochSecond();
        for (int i = 0; i < outcome.size(); i++) {
            Reward reward = outcome.get(i);
            boolean alt = alternative(i);
            final Reward target = reward;
            services.players().withOffline(id, data -> {
                if (!alt) services.winLimits().record(data, crate, target);
                if (!alt && !target.virtualReward() && !target.winItems().isEmpty()) {
                    data.addClaim(crate.id() + ";" + target.id() + ";" + now);
                }
            });
            java.util.List<String> commands = alt && reward.alternative().enabled()
                    ? reward.alternative().commands() : reward.winCommands();
            for (String command : commands) {
                String cmd = command.replace("%player%", player.getName());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                final String run = cmd;
                com.nocrates.compat.Scheduling.run(services.plugin(), null, () ->
                        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), run));
            }
            services.actionLogger().win(player.getName(), crate.id(), reward.id() + " (offline)");
        }
    }

    /** Releases the placement lock and closes the lid without granting (aborts). */
    public void release() {
        if (placement != null) {
            placement.setLidOpen(false);
            placement.unlock();
        }
    }
}
