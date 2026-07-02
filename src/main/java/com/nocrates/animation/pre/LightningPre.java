package com.nocrates.animation.pre;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Storm charge-up ending in a (visual-only) lightning strike on the crate. */
public final class LightningPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "LIGHTNING";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            Compat.spawn(Compat.particle("ELECTRIC_SPARK"), anchor.clone().add(0, 1.2, 0), 6, 0.6, 0.8, 0.6, 0.05);
            if (tick >= duration / 2 && tick - 2 < duration / 2) {
                anchor.getWorld().strikeLightningEffect(anchor.clone().subtract(0, 1, 0));
            }
        });
    }
}
