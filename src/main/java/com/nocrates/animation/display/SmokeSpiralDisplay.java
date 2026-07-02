package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Cosy campfire smoke spiraling around the reward. */
public final class SmokeSpiralDisplay extends ShowcaseBase implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "SMOKE_SPIRAL";
    }

    @Override
    public void play(OpeningContext ctx) {
        showcase(ctx, tick -> {
            Location anchor = ctx.anchor();
            double angle = tick * 0.25;
            double y = (tick % 50) / 50.0 * 1.8;
            Location at = anchor.clone().add(0.9 * Math.cos(angle), y, 0.9 * Math.sin(angle));
            Compat.spawn(Compat.particle("CAMPFIRE_COSY_SMOKE"), at, 1, 0, 0.02, 0, 0.005);
        });
    }
}
