package com.nocrates.animation;

import com.nocrates.compat.Scheduling;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.reward.Reward;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State handed to phase animations. Each phase calls {@link #phaseDone()} exactly once;
 * a watchdog force-advances after 20s so a broken animation can never eat a reward.
 */
public final class OpeningContext {

    public enum Phase {
        PRE, POST, DISPLAY, DONE
    }

    private final Plugin plugin;
    private final Player player;
    private final Crate crate;
    private final CratePlacement placement;
    private final List<Reward> outcome;
    private final Runnable onComplete;
    private final AtomicInteger phaseGuard = new AtomicInteger();
    private volatile Phase phase = Phase.PRE;
    private java.util.function.Consumer<Phase> phaseRunner;

    public OpeningContext(Plugin plugin, Player player, Crate crate, CratePlacement placement,
                          List<Reward> outcome, Runnable onComplete) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.placement = placement;
        this.outcome = outcome;
        this.onComplete = onComplete;
    }

    void phaseRunner(java.util.function.Consumer<Phase> runner) {
        this.phaseRunner = runner;
    }

    public Plugin plugin() {
        return plugin;
    }

    public Player player() {
        return player;
    }

    public Crate crate() {
        return crate;
    }

    /** Null for virtual opens (command-opened with no placed crate). */
    public CratePlacement placement() {
        return placement;
    }

    public List<Reward> outcome() {
        return outcome;
    }

    public Phase phase() {
        return phase;
    }

    /** Where effects should center: the crate block, or the player for virtual opens. */
    public Location anchor() {
        if (placement != null) {
            Location anchor = placement.effectAnchor();
            if (anchor != null) return anchor;
        }
        return player.getLocation().clone().add(0, 1.0, 0);
    }

    public ItemStack displayItem(Reward reward) {
        return reward.displayItem().build();
    }

    /** Advances PRE -> POST -> DISPLAY -> completion; safe to call once per phase. */
    public void phaseDone() {
        int expected = phase.ordinal();
        if (!phaseGuard.compareAndSet(expected, expected + 1)) return;
        Phase next = switch (phase) {
            case PRE -> Phase.POST;
            case POST -> Phase.DISPLAY;
            default -> Phase.DONE;
        };
        phase = next;
        Scheduling.run(plugin, anchor(), () -> {
            if (next == Phase.DONE) {
                onComplete.run();
            } else if (phaseRunner != null) {
                phaseRunner.accept(next);
            }
        });
    }

    /** Watchdog: force-advance if the given phase is still running after the timeout. */
    void watchdog(Phase watched, long timeoutTicks) {
        Scheduling.later(plugin, anchor(), timeoutTicks, () -> {
            if (phase == watched) {
                plugin.getLogger().warning("Animation phase " + watched + " for crate '" + crate.id()
                        + "' timed out — advancing.");
                phaseDone();
            }
        });
    }
}
