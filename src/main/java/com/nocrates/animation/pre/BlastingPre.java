package com.nocrates.animation.pre;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** TNT-style fuse: smoke builds up and the lid blows off. */
public final class BlastingPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "BLASTING";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Compat.playAt(ctx.anchor(), "ENTITY_TNT_PRIMED", 0.9f, 1f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            Compat.spawn(Compat.particle("SMOKE"), anchor.clone().add(0, 0.4, 0), 5, 0.15, 0.3, 0.15, 0.02);
            if (tick % 10 == 0) Compat.playAt(anchor, "BLOCK_NOTE_BLOCK_HAT", 0.7f, 1.6f);
            if (tick + 2 >= duration) {
                Compat.spawn(Compat.particle("EXPLOSION_EMITTER"), anchor, 1, 0, 0, 0, 0);
                Compat.playAt(anchor, "ENTITY_GENERIC_EXPLODE", 0.9f, 1.1f);
            }
        });
    }
}
