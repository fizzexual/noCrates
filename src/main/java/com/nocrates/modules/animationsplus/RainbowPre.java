package com.nocrates.modules.animationsplus;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Color;
import org.bukkit.Location;

/** Rainbow dust arcs sweeping across the crate. */
public final class RainbowPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "RAINBOW";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Compat.playAt(ctx.anchor(), "BLOCK_AMETHYST_BLOCK_CHIME", 0.9f, 1.2f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location anchor = ctx.anchor();
            // arc: half circle over the crate, hue cycling along the arc and over time
            for (int i = 0; i <= 8; i++) {
                double angle = Math.PI * i / 8.0;
                double sweep = tick * 0.1;
                double x = 1.4 * Math.cos(angle) * Math.cos(sweep);
                double z = 1.4 * Math.cos(angle) * Math.sin(sweep);
                double y = 1.2 * Math.sin(angle);
                float hue = ((tick * 8 + i * 30) % 360) / 360f;
                java.awt.Color awt = java.awt.Color.getHSBColor(hue, 0.9f, 1f);
                Compat.spawnDust(anchor.clone().add(x, y, z),
                        Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue()), 1.1f, 1, 0, 0, 0);
            }
        });
    }
}
