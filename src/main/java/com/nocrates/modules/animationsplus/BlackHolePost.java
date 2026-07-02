package com.nocrates.modules.animationsplus;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import com.nocrates.compat.Scheduling;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

/**
 * Black Hole (add-on parity): a dark vortex above the crate gently drags nearby
 * players toward it with a brief slowness — spectacle only, no damage.
 */
public final class BlackHolePost implements PostOpenAnimation {

    private static final double PULL_RANGE = 5.0;

    @Override
    public String id() {
        return "BLACK_HOLE";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        Compat.playAt(ctx.anchor(), "BLOCK_PORTAL_AMBIENT", 1f, 0.5f);
        Ticker.run(ctx, duration, 2, tick -> {
            Location center = ctx.anchor().clone().add(0, 1.2, 0);
            Compat.spawn(Compat.particle("SQUID_INK"), center, 8, 0.15, 0.15, 0.15, 0.02);
            Compat.spawn(Compat.particle("PORTAL"), center, 20, 0.1, 0.1, 0.1, 1.2);
            for (int i = 0; i < 6; i++) {
                double angle = tick * 0.3 + i * Math.PI / 3;
                double radius = 1.6 - (tick % 10) * 0.12;
                Location at = center.clone().add(radius * Math.cos(angle), (i - 3) * 0.15, radius * Math.sin(angle));
                Compat.spawn(Compat.particle("REVERSE_PORTAL"), at, 1, 0, 0, 0, 0);
            }
            if (tick % 6 == 0) {
                for (Player nearby : center.getNearbyPlayers(PULL_RANGE)) {
                    Vector pull = center.toVector().subtract(nearby.getLocation().toVector());
                    if (pull.lengthSquared() < 1.2) continue;
                    Vector velocity = pull.normalize().multiply(0.22);
                    var slowness = Compat.potionEffect("SLOWNESS");
                    Scheduling.entity(ctx.plugin(), nearby, () -> {
                        nearby.setVelocity(nearby.getVelocity().add(velocity));
                        if (slowness != null) {
                            nearby.addPotionEffect(new PotionEffect(slowness, 20, 0, true, false));
                        }
                    });
                }
            }
        });
    }
}
