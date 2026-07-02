package com.nocrates.modules.animationsplus;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;
import org.bukkit.entity.Frog;
import org.bukkit.util.Vector;

/** A frog leaps onto the crate and cannonballs into it. */
public final class FroggoBoomPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "FROGGO_BOOM";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = Math.max(30, ctx.crate().animation().preDelayTicks());
        Location anchor = ctx.anchor();
        Location spawnAt = anchor.clone().add(2.5, 0.5, 0);
        Frog frog = anchor.getWorld().spawn(spawnAt, Frog.class, f -> {
            f.setAI(false);
            f.setInvulnerable(true);
            f.setPersistent(false);
            f.setSilent(true);
            com.nocrates.animation.Displays.tag(f);
        });
        ctx.onPhaseCleanup(frog::remove);
        long hopAt = duration / 3;
        long diveAt = 2 * duration / 3;
        Ticker.run(ctx, duration, 2, tick -> {
            if (frog.isValid()) {
                if (tick < hopAt) {
                    frog.teleport(frog.getLocation().add(-0.12, 0, 0));
                    frog.getLocation().setDirection(anchor.toVector().subtract(frog.getLocation().toVector()));
                } else if (tick < diveAt) {
                    // hop up above the crate
                    Location above = anchor.clone().add(0, 1.4, 0);
                    Vector step = above.toVector().subtract(frog.getLocation().toVector()).multiply(0.25);
                    frog.teleport(frog.getLocation().add(step));
                } else {
                    frog.teleport(frog.getLocation().subtract(0, 0.25, 0));
                    if (frog.getLocation().getY() <= anchor.getY() - 0.4) {
                        frog.remove();
                        Compat.spawn(Compat.particle("POOF"), anchor, 8, 0.3, 0.2, 0.3, 0.02);
                        Compat.spawn(Compat.particle("SPLASH"), anchor, 12, 0.4, 0.2, 0.4, 0);
                        Compat.playAt(anchor, "ENTITY_FROG_LONG_JUMP", 1f, 0.8f);
                    }
                }
            }
            if (tick % 14 == 0 && frog.isValid()) {
                Compat.playAt(frog.getLocation(), "ENTITY_FROG_AMBIENT", 0.8f, 1f);
            }
        });
    }
}
