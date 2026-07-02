package com.nocrates.animation.pre;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Warden-style charge-up ending in a sonic boom across the crate. */
public final class SonicBoomPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "SONIC_BOOM";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            Compat.spawn(Compat.particle("SCULK_SOUL"), anchor.clone().add(0, 0.4, 0), 4, 0.4, 0.3, 0.4, 0.03);
            if (tick % 10 == 0) Compat.playAt(anchor, "ENTITY_WARDEN_HEARTBEAT", 0.9f, 1f);
            if (tick + 2 >= duration) {
                Compat.spawn(Compat.particle("SONIC_BOOM"), anchor.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                Compat.playAt(anchor, "ENTITY_WARDEN_SONIC_BOOM", 0.8f, 1.1f);
            }
        });
    }
}
