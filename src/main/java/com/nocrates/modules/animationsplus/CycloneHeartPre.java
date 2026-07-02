package com.nocrates.modules.animationsplus;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Hearts swirl in a tightening cyclone and dive into the crate. */
public final class CycloneHeartPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "CYCLONE_HEART";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            double progress = tick / (double) duration;
            double radius = 1.8 * (1.0 - progress) + 0.15;
            double height = 1.6 * (1.0 - progress);
            for (int arm = 0; arm < 3; arm++) {
                double angle = tick * 0.3 + arm * 2 * Math.PI / 3;
                Location at = anchor.clone().add(radius * Math.cos(angle), height, radius * Math.sin(angle));
                Compat.spawn(Compat.particle("HEART"), at, 1, 0, 0, 0, 0);
            }
            if (tick % 12 == 0) Compat.playAt(anchor, "ENTITY_ALLAY_ITEM_GIVEN", 0.6f, 1.5f);
        });
    }
}
