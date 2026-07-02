package com.nocrates.animation.post;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Twin portal-energy arms swirling upward from the opened crate. */
public final class SwirlPost implements PostOpenAnimation {

    @Override
    public String id() {
        return "SWIRL";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        Compat.playAt(ctx.anchor(), "BLOCK_PORTAL_TRIGGER", 0.4f, 1.6f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            double progress = tick / (double) duration;
            double y = progress * 2.2;
            double radius = 1.1 * (1.0 - progress * 0.5);
            for (int arm = 0; arm < 2; arm++) {
                double angle = tick * 0.35 + arm * Math.PI;
                Location at = anchor.clone().add(radius * Math.cos(angle), y, radius * Math.sin(angle));
                Compat.spawn(Compat.particle("PORTAL"), at, 4, 0.05, 0.05, 0.05, 0.02);
                Compat.spawn(Compat.particle("WITCH"), at, 1, 0, 0, 0, 0);
            }
        });
    }
}
