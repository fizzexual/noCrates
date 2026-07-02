package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Flames spiraling around the reward showcase. */
public final class FireSpiralDisplay extends ShowcaseBase implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "FIRE_SPIRAL";
    }

    @Override
    public void play(OpeningContext ctx) {
        Compat.playAt(ctx.anchor(), "ITEM_FIRECHARGE_USE", 0.5f, 1.3f);
        showcase(ctx, tick -> {
            Location anchor = ctx.anchor();
            for (int strand = 0; strand < 3; strand++) {
                double angle = tick * 0.28 + strand * 2 * Math.PI / 3;
                double y = (tick % 44) / 44.0 * 1.6;
                Location at = anchor.clone().add(0.85 * Math.cos(angle), y, 0.85 * Math.sin(angle));
                Compat.spawn(Compat.particle("FLAME"), at, 1, 0, 0, 0, 0.005);
            }
        });
    }
}
