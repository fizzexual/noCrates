package com.nocrates.animation.pre;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;

/** Enchantment glyphs streaming into the crate with a rising hum. */
public final class DefaultPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "DEFAULT";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Compat.playAt(ctx.anchor(), "BLOCK_ENCHANTMENT_TABLE_USE", 0.8f, 0.9f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            Compat.spawn(Compat.particle("ENCHANT"), anchor.clone().add(0, 0.6, 0), 12, 0.5, 0.4, 0.5, 0.6);
            if (tick % 10 == 0) {
                Compat.playAt(anchor, "BLOCK_NOTE_BLOCK_CHIME", 0.5f, 0.8f + tick / (float) duration);
            }
        });
    }
}
