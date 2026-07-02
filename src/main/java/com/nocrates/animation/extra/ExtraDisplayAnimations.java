package com.nocrates.animation.extra;

import com.nocrates.animation.Curves;
import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import com.nocrates.reward.Reward;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Eight choreographed reward showcases. Each spawns the reward display entities itself
 * (cleaned up via phase cleanups) and paints its signature effect around them.
 */
public final class ExtraDisplayAnimations {

    private ExtraDisplayAnimations() {
    }

    public static List<RewardDisplayAnimation> all() {
        return List.of(

                // The reward at the heart of a two-armed dust galaxy disc.
                new Showcase("GALAXY", "BLOCK_END_PORTAL_SPAWN", (ctx, tick) -> {
                    Location core = ctx.anchor().clone().add(0, 1.0, 0);
                    for (int arm = 0; arm < 2; arm++) {
                        for (int s = 1; s <= 7; s++) {
                            double a = tick * 0.07 + arm * Math.PI + s * 0.42;
                            double r = 0.28 * s;
                            Location at = core.clone().add(r * Math.cos(a), Math.sin(a * 2) * 0.05, r * Math.sin(a));
                            Compat.spawnDust(at, Curves.hue(0.65 + s * 0.03), 1.0f, 1, 0, 0, 0);
                        }
                    }
                    Compat.spawn(Compat.particle("END_ROD"), core, 1, 0.05, 0.05, 0.05, 0.002);
                }),

                // Winner on a podium of rotating spotlight beams.
                new Showcase("PODIUM", "UI_TOAST_CHALLENGE_COMPLETE", (ctx, tick) -> {
                    Location base = ctx.anchor().clone().subtract(0, 0.6, 0);
                    for (int spot = 0; spot < 3; spot++) {
                        double a = tick * 0.09 + spot * 2 * Math.PI / 3;
                        Location foot = base.clone().add(2.0 * Math.cos(a), 0, 2.0 * Math.sin(a));
                        for (Location at : Curves.line(foot, base.clone().add(0, 1.7, 0), 7)) {
                            Compat.spawnDust(at, Color.fromRGB(0xFFF3B0), 0.9f, 1, 0, 0, 0);
                        }
                    }
                }),

                // The reward rides a mini dust tornado.
                new Showcase("TORNADO", "ENTITY_BREEZE_IDLE_AIR", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    for (int step = 0; step < 8; step++) {
                        double h = ((tick * 0.05 + step * 0.125) % 1.0);
                        double a = tick * 0.4 + h * 8;
                        double r = 0.15 + h * 0.9;
                        Compat.spawnDust(anchor.clone().add(r * Math.cos(a), h * 1.6 - 0.6, r * Math.sin(a)),
                                Color.fromRGB(0xB9C4D0), 1.0f, 1, 0, 0, 0);
                    }
                }),

                // Waving aurora curtains above the reward.
                new Showcase("AURORA", "BLOCK_AMETHYST_BLOCK_RESONATE", (ctx, tick) -> {
                    Location top = ctx.anchor().clone().add(0, 2.0, 0);
                    for (int band = 0; band < 2; band++) {
                        for (int s = -5; s <= 5; s++) {
                            double x = s * 0.28;
                            double wave = Math.sin(tick * 0.12 + s * 0.55 + band * 1.7);
                            Location at = top.clone().add(x, wave * 0.35, band * 0.5 - 0.25 + wave * 0.1);
                            Compat.spawnDust(at, Curves.hue(0.35 + 0.12 * wave + band * 0.08), 1.2f, 1, 0, 0, 0);
                        }
                    }
                }),

                // A rotating triangular prism drawn in light around the reward.
                new Showcase("PRISM", "BLOCK_AMETHYST_CLUSTER_STEP", (ctx, tick) -> {
                    Location core = ctx.anchor().clone().add(0, 1.0, 0);
                    List<Vector> topRing = Curves.ring(0.9, 3, tick * 0.08);
                    List<Vector> bottomRing = Curves.ring(0.9, 3, tick * 0.08 + Math.PI / 3);
                    for (int i = 0; i < 3; i++) {
                        Location top = core.clone().add(topRing.get(i)).add(0, 0.7, 0);
                        Location bottom = core.clone().add(bottomRing.get(i)).subtract(0, 0.7, 0);
                        Location topNext = core.clone().add(topRing.get((i + 1) % 3)).add(0, 0.7, 0);
                        Location bottomNext = core.clone().add(bottomRing.get((i + 1) % 3)).subtract(0, 0.7, 0);
                        for (Location at : Curves.line(top, topNext, 4)) {
                            Compat.spawnDust(at, Color.fromRGB(0x9BE8FF), 0.9f, 1, 0, 0, 0);
                        }
                        for (Location at : Curves.line(bottom, bottomNext, 4)) {
                            Compat.spawnDust(at, Color.fromRGB(0x9BE8FF), 0.9f, 1, 0, 0, 0);
                        }
                        for (Location at : Curves.line(top, bottom, 4)) {
                            Compat.spawnDust(at, Color.fromRGB(0xE0F2FE), 0.8f, 1, 0, 0, 0);
                        }
                    }
                }),

                // Dark eclipse disc with a golden corona flaring behind the reward.
                new Showcase("ECLIPSE", "BLOCK_RESPAWN_ANCHOR_AMBIENT", (ctx, tick) -> {
                    Location core = ctx.anchor().clone().add(0, 1.2, 0);
                    Compat.spawn(Compat.particle("SQUID_INK"), core, 5, 0.12, 0.12, 0.02, 0.002);
                    double flare = 0.55 + 0.1 * Math.sin(tick * 0.2);
                    for (Vector p : Curves.ring(flare, 16, tick * 0.03)) {
                        // corona ring drawn in the vertical plane facing outward
                        Compat.spawnDust(core.clone().add(p.getX(), p.getZ(), 0),
                                Color.fromRGB(0xFFC145), 1.1f, 1, 0, 0, 0);
                    }
                }),

                // Little flame lanterns rise around the reward like a festival.
                new Showcase("LANTERNS", "BLOCK_CAMPFIRE_CRACKLE", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    java.util.Random rng = new java.util.Random(tick / 8 * 131L);
                    if (tick % 8 == 0) {
                        double a = rng.nextDouble() * Math.PI * 2;
                        double r = 0.8 + rng.nextDouble() * 1.0;
                        Location at = anchor.clone().add(r * Math.cos(a), 0, r * Math.sin(a));
                        for (int rise = 0; rise < 3; rise++) {
                            Compat.spawn(Compat.particle("FLAME"), at.clone().add(0, rise * 0.15, 0), 1, 0.01, 0.02, 0.01, 0.003);
                        }
                        Compat.spawnDust(at.clone().add(0, 0.35, 0), Color.fromRGB(0xFFD9A0), 1.0f, 1, 0, 0, 0);
                    }
                    // slow ambient rise of the whole lantern field
                    Compat.spawn(Compat.particle("SMALL_FLAME"), anchor.clone().add(0, 0.6 + (tick % 30) * 0.05, 0),
                            1, 0.6, 0.1, 0.6, 0.002);
                }),

                // A golden crown ring with gleam ticks above the reward.
                new Showcase("CROWN", "BLOCK_NOTE_BLOCK_BELL", (ctx, tick) -> {
                    Location head = ctx.anchor().clone().add(0, 1.75, 0);
                    List<Vector> ring = Curves.ring(0.45, 8, tick * 0.05);
                    for (int i = 0; i < ring.size(); i++) {
                        Vector p = ring.get(i);
                        double spike = i % 2 == 0 ? 0.22 : 0.05;
                        Compat.spawnDust(head.clone().add(p), Color.fromRGB(0xFFD24A), 1.1f, 1, 0, 0, 0);
                        Compat.spawnDust(head.clone().add(p).add(0, spike, 0), Color.fromRGB(0xFFE9A8), 0.9f, 1, 0, 0, 0);
                    }
                    if (tick % 20 == 0) {
                        Compat.spawn(Compat.particle("FIREWORK"), head, 3, 0.3, 0.1, 0.3, 0.01);
                        Compat.playAt(head, "BLOCK_NOTE_BLOCK_BELL", 0.5f, 1.8f);
                    }
                })
        );
    }

    /**
     * Shared showcase plumbing: fans the outcome items out with name tags, bobs and
     * spins them, runs the signature effect each tick, then cleans up and advances.
     */
    record Showcase(String id, String introSound, BiConsumer<OpeningContext, Integer> effect)
            implements RewardDisplayAnimation {

        @Override
        public void play(OpeningContext ctx) {
            Compat.playAt(ctx.anchor(), introSound, 0.9f, 1.1f);
            long duration = ctx.crate().animation().displayDurationTicks();
            List<Reward> rewards = ctx.outcome();
            List<ItemDisplay> items = new ArrayList<>();
            Location base = ctx.anchor().clone().add(0, 0.9, 0);
            for (int i = 0; i < rewards.size() && i < 5; i++) {
                double x = (i - (Math.min(rewards.size(), 5) - 1) / 2.0) * 0.8;
                Location at = base.clone().add(x, 0, 0);
                ItemDisplay item = Displays.item(at, ctx.displayItem(rewards.get(i)), 0.75f);
                items.add(item);
                ctx.onPhaseCleanup(item::remove);
                String name = rewards.get(i).displayName();
                if (name != null && !name.isEmpty()) {
                    var text = Displays.text(at.clone().add(0, 0.55, 0), name);
                    ctx.onPhaseCleanup(text::remove);
                }
            }
            final Location[] anchors = items.stream().map(ItemDisplay::getLocation).toArray(Location[]::new);
            Ticker.run(ctx, duration, 2, tick -> {
                for (int i = 0; i < items.size(); i++) {
                    ItemDisplay item = items.get(i);
                    if (!item.isValid()) continue;
                    Displays.spin(item, 10);
                    item.teleport(anchors[i].clone().add(0, Math.sin(tick * 0.12 + i) * 0.06, 0));
                }
                effect.accept(ctx, tick);
            });
        }
    }
}
