package com.nocrates.animation;

import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.reward.Reward;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registries for the three opening phases plus idle shapes, and the phase chain driver.
 * Animations registered by modules/addons appear automatically in the editor pickers.
 */
public final class AnimationService {

    private final Plugin plugin;
    private final Map<String, PreOpenAnimation> pre = new TreeMap<>();
    private final Map<String, PostOpenAnimation> post = new TreeMap<>();
    private final Map<String, RewardDisplayAnimation> display = new TreeMap<>();
    private final Map<String, IdleShape> shapes = new TreeMap<>();

    public AnimationService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(PreOpenAnimation a) {
        pre.put(a.id().toUpperCase(Locale.ROOT), a);
    }

    public void register(PostOpenAnimation a) {
        post.put(a.id().toUpperCase(Locale.ROOT), a);
    }

    public void register(RewardDisplayAnimation a) {
        display.put(a.id().toUpperCase(Locale.ROOT), a);
    }

    public void register(IdleShape shape) {
        shapes.put(shape.id().toUpperCase(Locale.ROOT), shape);
    }

    public java.util.Set<String> preIds() {
        return pre.keySet();
    }

    public java.util.Set<String> postIds() {
        return post.keySet();
    }

    public java.util.Set<String> displayIds() {
        return display.keySet();
    }

    public java.util.Optional<PreOpenAnimation> preById(String id) {
        return java.util.Optional.ofNullable(pre.get(id.toUpperCase(Locale.ROOT)));
    }

    public java.util.Optional<PostOpenAnimation> postById(String id) {
        return java.util.Optional.ofNullable(post.get(id.toUpperCase(Locale.ROOT)));
    }

    public java.util.Optional<RewardDisplayAnimation> displayById(String id) {
        return java.util.Optional.ofNullable(display.get(id.toUpperCase(Locale.ROOT)));
    }

    public IdleShape shape(String id) {
        return id == null ? null : shapes.get(id.toUpperCase(Locale.ROOT));
    }

    public java.util.Set<String> shapeIds() {
        return shapes.keySet();
    }

    /**
     * Plays the crate's three phases and completes with {@code onComplete}. Quick opens
     * skip straight to completion. Unknown animation ids are skipped gracefully.
     */
    public OpeningContext play(Player player, Crate crate, CratePlacement placement,
                               java.util.List<Reward> outcome, boolean quick, Runnable onComplete) {
        OpeningContext ctx = new OpeningContext(plugin, player, crate, placement, outcome, onComplete);
        if (quick) {
            if (placement != null) placement.setLidOpen(true);
            ctx.phaseDone(); // PRE
            ctx.phaseDone(); // POST
            ctx.phaseDone(); // DISPLAY -> complete
            return ctx;
        }
        ctx.phaseRunner(phase -> {
            switch (phase) {
                case POST -> {
                    if (placement != null) placement.setLidOpen(true);
                    runPost(ctx);
                }
                case DISPLAY -> runDisplay(ctx);
                default -> {
                }
            }
        });
        // watchdogs scale with the configured phase length so a long (legit) phase is
        // never force-ended while it is still playing
        ctx.watchdog(OpeningContext.Phase.PRE, crate.animation().preDelayTicks() + 400L);
        PreOpenAnimation animation = pre.get(crate.animation().preOpen().toUpperCase(Locale.ROOT));
        if (animation == null) {
            ctx.phaseDone();
        } else {
            animation.play(ctx);
        }
        return ctx;
    }

    private void runPost(OpeningContext ctx) {
        ctx.watchdog(OpeningContext.Phase.POST, ctx.crate().animation().postDelayTicks() + 400L);
        PostOpenAnimation animation = post.get(ctx.crate().animation().postOpen().toUpperCase(Locale.ROOT));
        if (animation == null) {
            ctx.phaseDone();
        } else {
            animation.play(ctx);
        }
    }

    private void runDisplay(OpeningContext ctx) {
        // chest-hunt style displays can legitimately run for minutes (player-paced)
        long timeout = Math.max(ctx.crate().animation().displayDurationTicks() + 400L, 20L * 90);
        ctx.watchdog(OpeningContext.Phase.DISPLAY, timeout);
        RewardDisplayAnimation animation = display.get(ctx.crate().animation().rewardDisplay().toUpperCase(Locale.ROOT));
        if (animation == null) {
            ctx.phaseDone();
        } else {
            animation.play(ctx);
        }
    }
}
