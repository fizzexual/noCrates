package com.nocrates.animation;

import com.nocrates.compat.Scheduling;
import com.nocrates.core.Services;
import com.nocrates.crate.CratePlacement;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the always-on idle effects. A slow global rescan diffs current placements
 * against running per-placement render timers (Folia-safe: each render timer is owned
 * by its region); rendering skips when no player is within render-radius.
 */
public final class IdleEffectTask {

    private static final long RENDER_PERIOD_TICKS = 2;

    private final Plugin plugin;
    private final AnimationService animations;
    private final Map<String, Scheduling.Cancellable> running = new ConcurrentHashMap<>();
    private final Map<String, List<EffectSpec>> parsedCache = new ConcurrentHashMap<>();
    private Scheduling.Cancellable rescanTask;

    public IdleEffectTask(Plugin plugin, AnimationService animations) {
        this.plugin = plugin;
        this.animations = animations;
    }

    public void start() {
        rescanTask = Scheduling.timer(plugin, null, 40, 100, this::rescan);
    }

    public void stop() {
        if (rescanTask != null) rescanTask.cancel();
        running.values().forEach(Scheduling.Cancellable::cancel);
        running.clear();
        parsedCache.clear();
    }

    public void invalidate() {
        parsedCache.clear();
        running.values().forEach(Scheduling.Cancellable::cancel);
        running.clear();
    }

    private void rescan() {
        var placements = Services.get().placements();
        // stop timers for removed placements
        for (String locKey : new ArrayList<>(running.keySet())) {
            if (placements.at(locKey) == null) {
                var task = running.remove(locKey);
                if (task != null) task.cancel();
            }
        }
        // start timers for new placements with effects
        for (CratePlacement placement : placements.all()) {
            if (running.containsKey(placement.locKey())) continue;
            if (placement.crate().animation().idleEffects().isEmpty()) continue;
            Location loc = placement.location();
            if (loc == null) continue;
            startRenderer(placement, loc);
        }
    }

    private void startRenderer(CratePlacement placement, Location loc) {
        final int[] tick = {0};
        Scheduling.Cancellable task = Scheduling.timer(plugin, loc, RENDER_PERIOD_TICKS, RENDER_PERIOD_TICKS, () -> {
            Location anchor = placement.effectAnchor();
            if (anchor == null) return;
            if (!anchor.getWorld().isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) return;
            int radius = Services.get().config().renderRadius();
            if (anchor.getNearbyPlayers(radius).isEmpty()) return;
            tick[0] += RENDER_PERIOD_TICKS;
            for (EffectSpec spec : effects(placement)) {
                ParticleBrush.render(spec, animations.shape(spec.shape()), anchor, tick[0]);
            }
        });
        running.put(placement.locKey(), task);
    }

    private List<EffectSpec> effects(CratePlacement placement) {
        return parsedCache.computeIfAbsent(placement.crate().id(), id -> {
            List<EffectSpec> specs = new ArrayList<>();
            for (String line : placement.crate().animation().idleEffects()) {
                try {
                    specs.add(EffectSpec.parse(line));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Crate '" + id + "': bad idle effect — " + e.getMessage());
                }
            }
            return specs;
        });
    }
}
