package com.nocrates.animation.post;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** An expanding sphere of light bursting out of the crate. */
public final class BallPost implements PostOpenAnimation {

    @Override
    public String id() {
        return "BALL";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        Compat.playAt(ctx.anchor(), "ENTITY_PLAYER_LEVELUP", 0.8f, 1.2f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor().clone().add(0, 0.4, 0);
            double radius = 1.8 * tick / (double) duration;
            for (int i = 0; i < 14; i++) {
                double theta = Math.acos(2.0 * ((i * 0.618) % 1.0) - 1.0);
                double phi = i * 2 * Math.PI * 0.618 + tick * 0.1;
                Location at = anchor.clone().add(
                        radius * Math.sin(theta) * Math.cos(phi),
                        radius * Math.cos(theta),
                        radius * Math.sin(theta) * Math.sin(phi));
                Compat.spawn(Compat.particle("END_ROD"), at, 1, 0, 0, 0, 0);
            }
        });
    }
}
