package com.nocrates.animation.extra;

import com.nocrates.animation.Curves;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.BiConsumer;

/** Nine choreographed post-open bursts. */
public final class ExtraPostAnimations {

    private ExtraPostAnimations() {
    }

    public static List<PostOpenAnimation> all() {
        return List.of(

                // Implode to a single point, hold, then detonate a full particle shell.
                new Simple("SUPERNOVA", "BLOCK_END_PORTAL_SPAWN", (ctx, tick) -> {
                    Location core = ctx.anchor().clone().add(0, 1.0, 0);
                    long duration = ctx.crate().animation().postDelayTicks();
                    double progress = tick / (double) duration;
                    if (progress < 0.6) {
                        double r = 2.2 * (1 - progress / 0.6);
                        for (Vector p : Curves.ring(r, 10, tick * 0.3)) {
                            Compat.spawn(Compat.particle("END_ROD"),
                                    core.clone().add(Curves.rotateY(p, tick * 0.05))
                                            .add(0, Math.sin(tick * 0.2 + p.getX()) * 0.4, 0),
                                    1, 0, 0, 0, 0);
                        }
                    } else if (progress < 0.7) {
                        Compat.spawnDust(core, Color.WHITE, 2.5f, 4, 0.05, 0.05, 0.05);
                    } else {
                        double r = (progress - 0.7) / 0.3 * 3.0;
                        for (int i = 0; i < 16; i++) {
                            double theta = Math.acos(2.0 * ((i * 0.618) % 1.0) - 1.0);
                            double phi = i * 2 * Math.PI * 0.618;
                            Compat.spawn(Compat.particle("FIREWORK"), core.clone().add(
                                    r * Math.sin(theta) * Math.cos(phi),
                                    r * Math.cos(theta),
                                    r * Math.sin(theta) * Math.sin(phi)), 1, 0, 0, 0, 0);
                        }
                        if (tick % 8 == 0) Compat.playAt(core, "ENTITY_GENERIC_EXPLODE", 0.7f, 1.4f);
                    }
                }),

                // Totem geyser with arcing fallout.
                new Simple("FOUNTAIN", "BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT", (ctx, tick) -> {
                    Location anchor = ctx.anchor().clone().add(0, 0.3, 0);
                    Compat.spawn(Compat.particle("TOTEM_OF_UNDYING"), anchor, 8, 0.1, 0.1, 0.1, 0.35);
                    for (Vector p : Curves.ring(1.2 + Math.sin(tick * 0.15) * 0.3, 6, tick * 0.2)) {
                        Compat.spawn(Compat.particle("FIREWORK"),
                                anchor.clone().add(p).add(0, 1.8, 0), 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }),

                // A DNA double-helix erupts out of the crate, rungs included.
                new Simple("DNA", "BLOCK_BEACON_ACTIVATE", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().postDelayTicks();
                    double height = 2.6 * Curves.ease(tick / (double) duration);
                    for (double y = 0; y <= height; y += 0.35) {
                        double a = y * 2.4 + tick * 0.12;
                        Vector s1 = new Vector(0.7 * Math.cos(a), y, 0.7 * Math.sin(a));
                        Vector s2 = new Vector(-0.7 * Math.cos(a), y, -0.7 * Math.sin(a));
                        Compat.spawnDust(anchor.clone().add(s1), Color.fromRGB(0x38BDF8), 1.1f, 1, 0, 0, 0);
                        Compat.spawnDust(anchor.clone().add(s2), Color.fromRGB(0xF472B6), 1.1f, 1, 0, 0, 0);
                        if (((int) (y / 0.35)) % 3 == 0) {
                            for (Location at : Curves.line(anchor.clone().add(s1), anchor.clone().add(s2), 4)) {
                                Compat.spawnDust(at, Color.fromRGB(0xE2E8F0), 0.8f, 1, 0, 0, 0);
                            }
                        }
                    }
                }),

                // Dust wings unfold behind the crate, then flap into a feather burst.
                new Simple("WINGS", "ENTITY_PHANTOM_FLAP", (ctx, tick) -> {
                    Location anchor = ctx.anchor().clone().add(0, 0.9, 0);
                    long duration = ctx.crate().animation().postDelayTicks();
                    double spread = Curves.ease(Math.min(1, tick / (duration * 0.6)));
                    double flap = Math.sin(tick * 0.25) * 0.35;
                    for (int side = -1; side <= 1; side += 2) {
                        for (int f = 1; f <= 5; f++) {
                            double reach = 0.35 * f * spread;
                            Location at = anchor.clone().add(
                                    side * reach,
                                    0.55 * spread - 0.1 * f + flap * (f / 5.0),
                                    -0.25 * f * spread);
                            Compat.spawnDust(at, Color.fromRGB(0xF8FAFC), 1.3f, 1, 0, 0, 0);
                            Compat.spawnDust(at.clone().add(0, -0.18, 0), Color.fromRGB(0x93C5FD), 1.0f, 1, 0, 0, 0);
                        }
                    }
                    if (tick + 2 >= duration) {
                        Compat.spawn(Compat.particle("CLOUD"), anchor, 20, 0.8, 0.4, 0.8, 0.08);
                        Compat.playAt(anchor, "ENTITY_ENDER_DRAGON_FLAP", 1f, 1.3f);
                    }
                }),

                // A ground shockwave ring races outward twice.
                new Simple("SHOCKWAVE", "ENTITY_GENERIC_EXPLODE", (ctx, tick) -> {
                    Location ground = ctx.anchor().clone().subtract(0, 0.9, 0);
                    long duration = ctx.crate().animation().postDelayTicks();
                    double wave = (tick % (duration / 2.0)) / (duration / 2.0);
                    double r = 0.4 + wave * 3.2;
                    for (Vector p : Curves.ring(r, (int) (10 + r * 6), 0)) {
                        Compat.spawn(Compat.particle("CLOUD"), ground.clone().add(p).add(0, 0.2, 0), 1, 0, 0.05, 0, 0.01);
                        Compat.spawnDust(ground.clone().add(p).add(0, 0.1, 0), Color.fromRGB(0xFFC145), 1.0f, 1, 0, 0, 0);
                    }
                    if (tick % (int) (duration / 2) == 2) {
                        Compat.playAt(ground, "ENTITY_GENERIC_EXPLODE", 0.5f, 1.6f);
                    }
                }),

                // Four corner pillars ignite in sequence, then fire beams into the center.
                new Simple("PILLARS", "BLOCK_RESPAWN_ANCHOR_CHARGE", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().postDelayTicks();
                    double progress = tick / (double) duration;
                    List<Vector> corners = Curves.ring(1.8, 4, Math.PI / 4);
                    int lit = (int) Math.min(4, Math.ceil(progress * 5));
                    for (int i = 0; i < lit; i++) {
                        Location base = anchor.clone().add(corners.get(i)).subtract(0, 0.8, 0);
                        for (double y = 0; y < 2.2; y += 0.3) {
                            Compat.spawn(Compat.particle("SOUL_FIRE_FLAME"),
                                    base.clone().add(0, y, 0), 1, 0.03, 0.03, 0.03, 0.005);
                        }
                    }
                    if (progress > 0.75) {
                        Location center = anchor.clone().add(0, 1.4, 0);
                        for (int i = 0; i < 4; i++) {
                            Location top = anchor.clone().add(corners.get(i)).add(0, 1.4, 0);
                            for (Location at : Curves.line(top, center, 5)) {
                                Compat.spawnDust(at, Color.fromRGB(0x7DF9FF), 1.0f, 1, 0, 0, 0);
                            }
                        }
                    }
                    if (tick % 10 == 0 && lit < 4) Compat.playAt(anchor, "BLOCK_RESPAWN_ANCHOR_CHARGE", 0.6f, 1.2f);
                }),

                // Bubbling water geyser with dolphin-grace splashes.
                new Simple("GEYSER", "ENTITY_DOLPHIN_SPLASH", (ctx, tick) -> {
                    Location anchor = ctx.anchor().clone().add(0, 0.2, 0);
                    Compat.spawn(Compat.particle("SPLASH"), anchor, 14, 0.25, 0.1, 0.25, 0.3);
                    Compat.spawn(Compat.particle("BUBBLE_POP"), anchor.clone().add(0, 1.0, 0), 6, 0.2, 0.5, 0.2, 0.05);
                    Compat.spawn(Compat.particle("DOLPHIN"), anchor.clone().add(0, 1.6, 0), 4, 0.3, 0.3, 0.3, 0.01);
                    if (tick % 12 == 0) Compat.playAt(anchor, "ENTITY_DOLPHIN_SPLASH", 0.7f, 1.1f);
                }),

                // A flaming comet orbits fast and climbs into the sky.
                new Simple("COMET_RING", "ENTITY_BLAZE_SHOOT", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().postDelayTicks();
                    double progress = tick / (double) duration;
                    double a = tick * 0.5;
                    double r = 1.7 - progress * 0.8;
                    Location head = anchor.clone().add(r * Math.cos(a), 0.4 + progress * 2.2, r * Math.sin(a));
                    Compat.spawn(Compat.particle("FLAME"), head, 4, 0.05, 0.05, 0.05, 0.01);
                    for (int trail = 1; trail <= 4; trail++) {
                        double ta = a - trail * 0.3;
                        Location tp = anchor.clone().add(r * Math.cos(ta), 0.4 + progress * 2.2 - trail * 0.04, r * Math.sin(ta));
                        Compat.spawnDust(tp, Color.fromRGB(0xFF9A3C), 1.1f - trail * 0.18f, 1, 0, 0, 0);
                    }
                    if (tick + 2 >= duration) {
                        Compat.spawn(Compat.particle("FLASH"), head, 1, 0, 0, 0, 0);
                    }
                }),

                // Stars rain down in a ring and splash sparks where they land.
                new Simple("STARFALL", "BLOCK_AMETHYST_BLOCK_CHIME", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    java.util.Random rng = new java.util.Random(tick / 6 * 977L);
                    if (tick % 6 == 0) {
                        Vector p = Curves.ring(1.6, 8, rng.nextDouble() * 6.28)
                                .get(rng.nextInt(8));
                        for (Location at : Curves.line(
                                anchor.clone().add(p).add(0, 3.2, 0),
                                anchor.clone().add(p), 6)) {
                            Compat.spawn(Compat.particle("END_ROD"), at, 1, 0, 0, 0, 0);
                        }
                        Compat.spawn(Compat.particle("FIREWORK"), anchor.clone().add(p), 6, 0.1, 0.05, 0.1, 0.04);
                        Compat.playAt(anchor, "BLOCK_AMETHYST_BLOCK_CHIME", 0.7f,
                                0.8f + rng.nextFloat() * 0.8f);
                    }
                })
        );
    }

    record Simple(String id, String introSound, BiConsumer<OpeningContext, Integer> body)
            implements PostOpenAnimation {

        @Override
        public void play(OpeningContext ctx) {
            Compat.playAt(ctx.anchor(), introSound, 0.9f, 1f);
            Ticker.run(ctx, ctx.crate().animation().postDelayTicks(), 2,
                    tick -> body.accept(ctx, tick));
        }
    }
}
