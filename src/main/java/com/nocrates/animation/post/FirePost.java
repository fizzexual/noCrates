package com.nocrates.animation.post;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** A roaring flame fountain out of the open crate. */
public final class FirePost implements PostOpenAnimation {

    @Override
    public String id() {
        return "FIRE";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        Compat.playAt(ctx.anchor(), "ENTITY_BLAZE_SHOOT", 0.8f, 0.9f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor().clone().add(0, 0.3, 0);
            Compat.spawn(Compat.particle("FLAME"), anchor, 10, 0.15, 0.1, 0.15, 0.12);
            Compat.spawn(Compat.particle("LAVA"), anchor, 2, 0.2, 0.1, 0.2, 0);
            if (tick % 8 == 0) Compat.playAt(anchor, "BLOCK_FIRE_AMBIENT", 0.9f, 0.8f);
        });
    }
}
