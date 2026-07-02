package com.nocrates.animation;

import com.nocrates.compat.Scheduling;

import java.util.function.IntConsumer;

/** Runs a visual body every {@code period} ticks for {@code duration}, then advances the phase. */
public final class Ticker {

    private Ticker() {
    }

    public static void run(OpeningContext ctx, long durationTicks, long period, IntConsumer body) {
        // Bind completion to the phase this ticker belongs to: if the watchdog already
        // force-advanced it, this ticker's end must not chop the NEXT phase short.
        int snapshot = ctx.guard();
        run(ctx, durationTicks, period, body, () -> ctx.phaseDoneIf(snapshot));
    }

    public static void run(OpeningContext ctx, long durationTicks, long period, IntConsumer body, Runnable onEnd) {
        final int[] tick = {0};
        final Scheduling.Cancellable[] handle = new Scheduling.Cancellable[1];
        handle[0] = Scheduling.timer(ctx.plugin(), ctx.anchor(), period, period, () -> {
            tick[0] += (int) period;
            try {
                body.accept(tick[0]);
            } catch (Exception e) {
                ctx.plugin().getLogger().warning("Animation tick failed: " + e.getMessage());
                tick[0] = (int) durationTicks;
            }
            if (tick[0] >= durationTicks) {
                if (handle[0] != null) handle[0].cancel();
                onEnd.run();
            }
        });
    }
}
