package com.nocrates.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * Single scheduling facade for Paper and Folia. On Folia, tasks are routed to the
 * region/entity/async schedulers (required — BukkitScheduler throws there); everywhere
 * else the classic BukkitScheduler is used. The Folia scheduler API exists in paper-api
 * since 1.20.1, so the calls below compile against our build target and only execute
 * when Folia is detected at runtime.
 */
public final class Scheduling {

    public static final boolean FOLIA = detectFolia();

    private Scheduling() {
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public interface Cancellable {
        void cancel();
    }

    /** Runs on the thread owning {@code ctx} (Folia) or the main thread. Null ctx = global. */
    public static void run(Plugin plugin, Location ctx, Runnable r) {
        if (FOLIA) {
            if (ctx != null) Bukkit.getRegionScheduler().run(plugin, ctx, t -> r.run());
            else Bukkit.getGlobalRegionScheduler().run(plugin, t -> r.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    public static void later(Plugin plugin, Location ctx, long ticks, Runnable r) {
        long delay = Math.max(1, ticks);
        if (FOLIA) {
            if (ctx != null) Bukkit.getRegionScheduler().runDelayed(plugin, ctx, t -> r.run(), delay);
            else Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> r.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, r, delay);
        }
    }

    public static Cancellable timer(Plugin plugin, Location ctx, long delayTicks, long periodTicks, Runnable r) {
        long delay = Math.max(1, delayTicks);
        long period = Math.max(1, periodTicks);
        if (FOLIA) {
            if (ctx != null) {
                var task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, ctx, t -> r.run(), delay, period);
                return task::cancel;
            }
            var task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> r.run(), delay, period);
            return task::cancel;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, r, delay, period);
        return task::cancel;
    }

    /** Runs on the thread owning the entity (Folia) or the main thread. */
    public static void entity(Plugin plugin, Entity e, Runnable r) {
        if (FOLIA) {
            e.getScheduler().run(plugin, t -> r.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    public static void entityLater(Plugin plugin, Entity e, long ticks, Runnable r) {
        long delay = Math.max(1, ticks);
        if (FOLIA) {
            e.getScheduler().runDelayed(plugin, t -> r.run(), null, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, r, delay);
        }
    }

    public static void async(Plugin plugin, Runnable r) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> r.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
        }
    }

    public static Cancellable asyncTimer(Plugin plugin, long delayTicks, long periodTicks, Runnable r) {
        if (FOLIA) {
            var task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> r.run(),
                    Math.max(1, delayTicks) * 50, Math.max(1, periodTicks) * 50, TimeUnit.MILLISECONDS);
            return task::cancel;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, r, Math.max(1, delayTicks), Math.max(1, periodTicks));
        return task::cancel;
    }
}
