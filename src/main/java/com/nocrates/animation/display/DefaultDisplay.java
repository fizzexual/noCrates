package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.compat.Compat;

/** The classic hover: won item(s) floating over the crate with name tags. */
public final class DefaultDisplay extends ShowcaseBase implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "DEFAULT";
    }

    @Override
    public void play(OpeningContext ctx) {
        Compat.playAt(ctx.anchor(), "ENTITY_EXPERIENCE_ORB_PICKUP", 0.9f, 1.1f);
        showcase(ctx, tick -> {
            if (tick % 10 == 0) {
                Compat.spawn(Compat.particle("HAPPY_VILLAGER"), ctx.anchor().clone().add(0, 1.0, 0),
                        3, 0.6, 0.4, 0.6, 0);
            }
        });
    }
}
