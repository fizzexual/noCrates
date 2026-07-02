package com.nocrates.modules.animationsplus;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Location;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;

/** A chicken jockey charges the opener (harmlessly) and vanishes into the crate. */
public final class ChickenJockeyPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "CHICKEN_JOCKEY";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = Math.max(30, ctx.crate().animation().preDelayTicks());
        Location anchor = ctx.anchor();
        Location spawnAt = anchor.clone().add(-2.5, 0.2, -1.5);
        Chicken chicken = anchor.getWorld().spawn(spawnAt, Chicken.class, c -> {
            c.setAI(false);
            c.setInvulnerable(true);
            c.setPersistent(false);
        });
        Zombie rider = anchor.getWorld().spawn(spawnAt, Zombie.class, z -> {
            z.setBaby();
            z.setAI(false);
            z.setInvulnerable(true);
            z.setPersistent(false);
            z.setSilent(true);
            z.setShouldBurnInDay(false);
        });
        chicken.addPassenger(rider);
        ctx.onPhaseCleanup(chicken::remove);
        ctx.onPhaseCleanup(rider::remove);
        long chargeUntil = duration / 2;
        Ticker.run(ctx, duration, 2, tick -> {
            if (!chicken.isValid()) return;
            Location target = tick < chargeUntil
                    ? ctx.player().getLocation()
                    : anchor.clone().subtract(0, 0.3, 0);
            Vector step = target.toVector().subtract(chicken.getLocation().toVector());
            if (step.lengthSquared() > 0.04) {
                chicken.teleport(chicken.getLocation().add(step.normalize().multiply(0.28))
                        .setDirection(step));
            } else if (tick >= chargeUntil) {
                chicken.remove();
                rider.remove();
                Compat.spawn(Compat.particle("POOF"), anchor, 10, 0.3, 0.3, 0.3, 0.02);
                Compat.playAt(anchor, "ENTITY_CHICKEN_DEATH", 0.9f, 1.2f);
            }
            if (tick % 10 == 0 && chicken.isValid()) {
                Compat.playAt(chicken.getLocation(), "ENTITY_CHICKEN_AMBIENT", 0.9f, 1.1f);
                Compat.spawn(Compat.particle("CLOUD"), chicken.getLocation(), 2, 0.1, 0.05, 0.1, 0.01);
            }
        });
    }
}
