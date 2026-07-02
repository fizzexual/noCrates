package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.compat.Compat;
import org.bukkit.Color;
import org.bukkit.Location;

/** A double helix of colored dust climbing around the showcased reward. */
public final class HelixDisplay extends ShowcaseBase implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "HELIX";
    }

    @Override
    public void play(OpeningContext ctx) {
        Compat.playAt(ctx.anchor(), "BLOCK_BEACON_ACTIVATE", 0.6f, 1.4f);
        showcase(ctx, tick -> {
            Location anchor = ctx.anchor();
            double y = (tick % 40) / 40.0 * 2.0;
            for (int strand = 0; strand < 2; strand++) {
                double angle = tick * 0.3 + strand * Math.PI;
                Location at = anchor.clone().add(0.8 * Math.cos(angle), y, 0.8 * Math.sin(angle));
                Compat.spawnDust(at, strand == 0 ? Color.fromRGB(0x7B5CFF) : Color.fromRGB(0xFF5CA8),
                        1.1f, 1, 0, 0, 0);
            }
        });
    }
}
