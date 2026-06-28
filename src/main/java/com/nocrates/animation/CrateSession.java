package com.nocrates.animation;

import com.nocrates.crate.Crate;
import com.nocrates.reward.Reward;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * One opening in progress: the player, the crate, the pre-rolled outcome, and a
 * one-shot {@link #finish()} callback that grants the rewards. {@code finish()}
 * is idempotent so an animation can call it on normal completion or early exit
 * without double-granting.
 */
public final class CrateSession {

    private final Player player;
    private final Crate crate;
    private final List<Reward> outcome;
    private final Runnable onFinish;
    private boolean finished;

    public CrateSession(Player player, Crate crate, List<Reward> outcome, Runnable onFinish) {
        this.player = player;
        this.crate = crate;
        this.outcome = List.copyOf(outcome);
        this.onFinish = onFinish;
    }

    public Player player() {
        return player;
    }

    public Crate crate() {
        return crate;
    }

    public List<Reward> outcome() {
        return outcome;
    }

    public Reward primary() {
        return outcome.isEmpty() ? null : outcome.get(0);
    }

    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        onFinish.run();
    }
}
