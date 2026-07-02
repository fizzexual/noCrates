package com.nocrates.animation.pre;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** A ring of fire tightens around the crate until it ignites. */
public final class FirePre implements PreOpenAnimation {

    @Override
    public String id() {
        return "FIRE";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Compat.playAt(ctx.anchor(), "ITEM_FIRECHARGE_USE", 0.7f, 0.8f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            double progress = tick / (double) duration;
            double radius = 1.6 * (1.0 - progress) + 0.2;
            for (int i = 0; i < 8; i++) {
                double angle = tick * 0.2 + i * Math.PI / 4;
                Location at = anchor.clone().add(radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
                Compat.spawn(Compat.particle("FLAME"), at, 1, 0, 0.05, 0, 0.01);
            }
            if (tick % 12 == 0) Compat.playAt(anchor, "BLOCK_FIRE_AMBIENT", 0.8f, 1f);
        });
    }
}
