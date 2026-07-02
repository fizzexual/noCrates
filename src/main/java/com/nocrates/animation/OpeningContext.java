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
    private final java.util.List<Runnable> cleanups = new java.util.concurrent.CopyOnWriteArrayList<>();
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

    /** Registers cleanup (entity removal etc.) executed when the current phase ends. */
    public void onPhaseCleanup(Runnable cleanup) {
        cleanups.add(cleanup);
    }

    /** Current guard value; pair with {@link #phaseDoneIf(int)} to bind a completion to its phase. */
    public int guard() {
        return phaseGuard.get();
    }

    /**
     * Phase-bound completion: only advances if no one else (watchdog, another timer)
     * advanced the phase since {@code snapshot} was taken. Prevents a straggling timer
     * from a force-ended phase chopping the NEXT phase short.
     */
    public void phaseDoneIf(int snapshot) {
        if (phaseGuard.get() != snapshot) return;
        phaseDone();
    }

    /** Advances PRE -> POST -> DISPLAY -> completion; safe to call once per phase. */
    public void phaseDone() {
        int expected = phase.ordinal();
        if (expected >= Phase.DONE.ordinal()) return;
        if (!phaseGuard.compareAndSet(expected, expected + 1)) return;
        for (Runnable cleanup : cleanups) {
            try {
                cleanup.run();
            } catch (Exception ignored) {
            }
        }
        cleanups.clear();
        Phase next = switch (phase) {
            case PRE -> Phase.POST;
            case POST -> Phase.DISPLAY;
            default -> Phase.DONE;
        };
        phase = next;
        if (next == Phase.DONE) {
            // completion touches the PLAYER (inventory, menus) — run on their thread;
            // if they logged out, grantAll()'s offline path handles it, so run globally.
            if (player.isOnline()) {
                Scheduling.entity(plugin, player, onComplete);
            } else {
                Scheduling.run(plugin, null, onComplete);
            }
        } else {
            Scheduling.run(plugin, anchor(), () -> {
                if (phaseRunner != null) phaseRunner.accept(next);
            });
        }
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
