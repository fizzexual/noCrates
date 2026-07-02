package com.nocrates.animation.pre;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** The crate strains and cracks open — intensifying crit sparks and breaking sounds. */
public final class CrackPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "CRACK";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Ticker.run(ctx, duration, 3, tick -> {
            Location anchor = ctx.anchor();
            double intensity = tick / (double) duration;
            Compat.spawn(Compat.particle("CRIT"), anchor, (int) (4 + 10 * intensity), 0.3, 0.3, 0.3, 0.1);
            Compat.playAt(anchor, "BLOCK_WOOD_HIT", 0.7f, 0.6f + (float) intensity * 0.8f);
            if (tick + 3 >= duration) {
                Compat.spawn(Compat.particle("EXPLOSION"), anchor, 1, 0, 0, 0, 0);
                Compat.playAt(anchor, "BLOCK_WOOD_BREAK", 1f, 0.7f);
            }
        });
    }
}
