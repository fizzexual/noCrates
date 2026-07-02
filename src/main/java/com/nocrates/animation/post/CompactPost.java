package com.nocrates.animation.post;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Energy imploding into the crate before the reward appears. */
public final class CompactPost implements PostOpenAnimation {

    @Override
    public String id() {
        return "COMPACT";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor().clone().add(0, 0.5, 0);
            double progress = tick / (double) duration;
            double radius = 2.0 * (1.0 - progress) + 0.1;
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5 + tick * 0.15;
                double y = Math.sin(angle * 2 + tick * 0.1) * 0.6;
                Location at = anchor.clone().add(radius * Math.cos(angle), y, radius * Math.sin(angle));
                Compat.spawn(Compat.particle("ENCHANTED_HIT"), at, 1, 0, 0, 0, 0);
            }
            if (tick + 2 >= duration) {
                Compat.spawn(Compat.particle("FLASH"), anchor, 1, 0, 0, 0, 0);
                Compat.playAt(anchor, "ENTITY_ILLUSIONER_MIRROR_MOVE", 0.8f, 1.4f);
            }
        });
    }
}
