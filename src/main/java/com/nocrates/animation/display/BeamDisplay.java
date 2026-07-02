package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.compat.Compat;
import org.bukkit.Color;
import org.bukkit.Location;

/** A beacon-like pillar of light with expanding ground rings under the showcased reward. */
public final class BeamDisplay extends ShowcaseBase implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "BEAM";
    }

    @Override
    public void play(OpeningContext ctx) {
        Compat.playAt(ctx.anchor(), "BLOCK_BEACON_POWER_SELECT", 0.8f, 1.2f);
        showcase(ctx, tick -> {
            Location anchor = ctx.anchor();
            // the pillar
            for (int i = 0; i < 5; i++) {
                double y = ((tick * 0.18) + i * 1.1) % 5.5;
                Compat.spawn(Compat.particle("END_ROD"), anchor.clone().add(0, y, 0), 1, 0.03, 0.05, 0.03, 0);
            }
            Compat.spawnDust(anchor.clone().add(0, (tick % 40) / 40.0 * 5.0, 0),
                    Color.fromRGB(0x9BE8FF), 1.6f, 2, 0.05, 0.2, 0.05);
            // expanding ground ring every second
            double ringT = (tick % 20) / 20.0;
            double ringR = 0.4 + ringT * 1.8;
            for (int i = 0; i < 14; i++) {
                double angle = i * Math.PI / 7;
                Location at = anchor.clone().add(ringR * Math.cos(angle), -0.9, ringR * Math.sin(angle));
                Compat.spawnDust(at, Color.fromRGB(0x2F6FED), 1.0f, 1, 0, 0, 0);
            }
            if (tick % 20 == 0) {
                Compat.playAt(anchor, "BLOCK_BEACON_AMBIENT", 0.7f, 1.4f);
            }
        });
    }
}
