package com.nocrates.modules.animationsplus;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Astro Burst (add-on parity): energy rises from the crate and detonates radially. */
public final class AstroBurstPost implements PostOpenAnimation {

    @Override
    public String id() {
        return "ASTRO_BURST";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        long riseUntil = duration / 2;
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            if (tick <= riseUntil) {
                double y = 1.8 * tick / (double) riseUntil;
                Compat.spawn(Compat.particle("END_ROD"), anchor.clone().add(0, y, 0), 3, 0.1, 0.05, 0.1, 0.01);
                Compat.spawn(Compat.particle("FIREWORK"), anchor.clone().add(0, y, 0), 1, 0.05, 0.05, 0.05, 0.02);
            } else {
                double progress = (tick - riseUntil) / (double) (duration - riseUntil);
                double radius = 2.2 * progress;
                Location center = anchor.clone().add(0, 1.8, 0);
                for (int i = 0; i < 10; i++) {
                    double theta = i * Math.PI / 5;
                    double phi = (i % 2 == 0 ? 0.4 : -0.3) + tick * 0.05;
                    Location at = center.clone().add(
                            radius * Math.cos(theta) * Math.cos(phi),
                            radius * Math.sin(phi),
                            radius * Math.sin(theta) * Math.cos(phi));
                    Compat.spawn(Compat.particle("END_ROD"), at, 1, 0, 0, 0, 0);
                }
                if (tick - 2 <= riseUntil) {
                    Compat.playAt(anchor, "ENTITY_FIREWORK_ROCKET_LARGE_BLAST", 1f, 1.1f);
                }
            }
        });
    }
}
