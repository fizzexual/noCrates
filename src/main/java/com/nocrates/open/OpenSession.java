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
        return useAlternative[index];
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
        var winLimits = services.winLimits();
        var data = services.players().of(player);
        for (int i = 0; i < outcome.size(); i++) {
            Reward reward = outcome.get(i);
            if (!useAlternative[i]) winLimits.record(data, crate, reward);
            RewardGrant.grant(player, crate, reward, useAlternative[i]);
        }
        release();
    }

    /** Releases the placement lock and closes the lid without granting (aborts). */
    public void release() {
        if (placement != null) {
            placement.setLidOpen(false);
            placement.unlock();
        }
    }
}
