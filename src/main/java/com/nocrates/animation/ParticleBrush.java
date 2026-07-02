package com.nocrates.animation;

import com.nocrates.compat.Compat;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;

/** Renders one parsed idle effect for a tick. */
public final class ParticleBrush {

    private ParticleBrush() {
    }

    public static void render(EffectSpec spec, IdleShape shape, Location anchor, int tick) {
        Particle particle = Compat.particle(spec.particle());
        if (particle == null || shape == null) return;
        List<Vector> points = shape.points(spec.radius(), tick);
        boolean dust = Compat.isDust(particle);
        for (Vector point : points) {
            Location at = anchor.clone().add(
                    spec.offX() + point.getX(),
                    spec.offY() + point.getY(),
                    spec.offZ() + point.getZ());
            if (dust) {
                Compat.spawnDust(at, spec.bukkitColor(), 1.0f, spec.amount(), 0, 0, 0);
            } else {
                Compat.spawn(particle, at, spec.amount(), 0, 0, 0, spec.velocity());
            }
        }
    }
}
