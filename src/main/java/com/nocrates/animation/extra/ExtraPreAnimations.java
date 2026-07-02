package com.nocrates.animation.extra;

import com.nocrates.animation.Curves;
import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Nine choreographed pre-open animations. Each is a named {@link Simple} whose body
 * runs every 2 ticks for the crate's configured pre-open duration.
 */
public final class ExtraPreAnimations {

    private ExtraPreAnimations() {
    }

    public static List<PreOpenAnimation> all() {
        return List.of(

                // A dust tornado funnel: three arms spiral down from wide to narrow.
                new Simple("VORTEX", "ENTITY_EVOKER_CAST_SPELL", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    double progress = tick / (double) duration;
                    for (int arm = 0; arm < 3; arm++) {
                        for (int step = 0; step < 3; step++) {
                            double h = ((tick * 0.06 + step * 0.33) % 1.0);
                            double y = 2.4 * (1 - h);
                            double radius = (0.25 + 1.5 * (1 - h)) * (1 - progress * 0.4);
                            double a = tick * 0.35 + arm * 2 * Math.PI / 3 + h * 6;
                            Compat.spawnDust(anchor.clone().add(radius * Math.cos(a), y, radius * Math.sin(a)),
                                    Color.fromRGB(0x9BE8FF), 1.1f, 1, 0, 0, 0);
                        }
                    }
                    if (tick % 14 == 0) Compat.playAt(anchor, "ENTITY_BREEZE_IDLE_AIR", 0.7f, 0.8f);
                }),

                // Storm charge: crackling ring of sparks, mini-arcs, one real strike at the end.
                new Simple("THUNDER_STORM", "ENTITY_CREEPER_PRIMED", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    for (Vector p : Curves.ring(1.6, 4, tick * 0.2)) {
                        Compat.spawn(Compat.particle("ELECTRIC_SPARK"),
                                anchor.clone().add(p).add(0, 1.6, 0), 2, 0.1, 0.4, 0.1, 0.05);
                    }
                    if (tick % 10 == 0) {
                        // small arc: a jagged dust line from the cloud ring to the crate
                        Vector from = Curves.ring(1.6, 1, tick * 0.7).get(0).add(new Vector(0, 1.8, 0));
                        for (Location at : Curves.line(anchor.clone().add(from), anchor, 6)) {
                            Compat.spawnDust(at.add((Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2),
                                    Color.WHITE, 1.3f, 1, 0, 0, 0);
                        }
                        Compat.playAt(anchor, "ENTITY_FIREWORK_ROCKET_TWINKLE", 0.5f, 1.6f);
                    }
                    if (tick + 2 >= duration) {
                        anchor.getWorld().strikeLightningEffect(anchor.clone().subtract(0, 1, 0));
                    }
                }),

                // A blazing meteor falls from the sky and slams into the crate.
                new Simple("METEOR", "ENTITY_BLAZE_AMBIENT", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    double progress = Curves.ease(tick / (double) duration);
                    Location start = anchor.clone().add(6, 9, 6);
                    Location at = Curves.line(start, anchor, 100)
                            .get((int) Math.min(100, progress * 100));
                    Compat.spawn(Compat.particle("FLAME"), at, 6, 0.15, 0.15, 0.15, 0.02);
                    Compat.spawn(Compat.particle("LAVA"), at, 1, 0.1, 0.1, 0.1, 0);
                    Compat.spawn(Compat.particle("SMOKE"), at.clone().add(0.3, 0.4, 0.3), 3, 0.2, 0.2, 0.2, 0.01);
                    if (tick + 2 >= duration) {
                        Compat.spawn(Compat.particle("EXPLOSION_EMITTER"), anchor, 1, 0, 0, 0, 0);
                        Compat.spawn(Compat.particle("LAVA"), anchor, 12, 0.6, 0.3, 0.6, 0);
                        Compat.playAt(anchor, "ENTITY_GENERIC_EXPLODE", 1f, 0.8f);
                    }
                }),

                // Digital glitch: scattered two-tone blinks, pitch-shifted clicks, a final "reboot" flash.
                new Simple("GLITCH", "BLOCK_SCULK_SENSOR_CLICKING", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    java.util.Random rng = new java.util.Random(tick * 31L);
                    for (int i = 0; i < 8; i++) {
                        Location at = anchor.clone().add(
                                (rng.nextDouble() - 0.5) * 2.2,
                                rng.nextDouble() * 2.0,
                                (rng.nextDouble() - 0.5) * 2.2);
                        Compat.spawnDust(at, rng.nextBoolean() ? Color.fromRGB(0x00FFC8) : Color.fromRGB(0xFF0055),
                                1.2f, 1, 0, 0, 0);
                    }
                    if (tick % 6 == 0) {
                        Compat.playAt(anchor, "BLOCK_SCULK_SENSOR_CLICKING", 0.6f,
                                0.5f + (float) Math.random() * 1.5f);
                    }
                    if (tick + 2 >= duration) {
                        Compat.spawn(Compat.particle("FLASH"), anchor, 1, 0, 0, 0, 0);
                    }
                }),

                // Accelerating red pulse rings synced to a heartbeat.
                new Simple("HEARTBEAT", "ENTITY_WARDEN_HEARTBEAT", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    double progress = tick / (double) duration;
                    int beatEvery = (int) Math.max(4, 16 - progress * 12);
                    int sinceBeat = (int) (tick % beatEvery);
                    if (sinceBeat == 0) {
                        Compat.playAt(anchor, "ENTITY_WARDEN_HEARTBEAT", 1f, 0.8f + (float) progress);
                    }
                    double r = 0.3 + sinceBeat * 0.25;
                    for (Vector p : Curves.ring(r, 14, 0)) {
                        Compat.spawnDust(anchor.clone().add(p).add(0, 0.1, 0),
                                Color.fromRGB(0xE7263C), 1.2f, 1, 0, 0, 0);
                    }
                }),

                // A particle cage assembles bar by bar, then shatters.
                new Simple("CAGE", "BLOCK_ANVIL_PLACE", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    double progress = tick / (double) duration;
                    int bars = 8;
                    int built = (int) Math.ceil(bars * Math.min(1.0, progress * 1.4));
                    List<Vector> posts = Curves.ring(1.3, bars, 0);
                    for (int i = 0; i < built; i++) {
                        Vector p = posts.get(i);
                        for (double y = 0; y <= 2.0; y += 0.4) {
                            Compat.spawnDust(anchor.clone().add(p).add(0, y - 0.8, 0),
                                    Color.fromRGB(0xC8CDD4), 1.0f, 1, 0, 0, 0);
                        }
                    }
                    if (tick % 8 == 0 && built < bars) Compat.playAt(anchor, "BLOCK_ANVIL_PLACE", 0.4f, 1.6f);
                    if (tick + 2 >= duration) {
                        Compat.spawn(Compat.particle("CRIT"), anchor.clone().add(0, 0.6, 0), 30, 1.2, 1.0, 1.2, 0.2);
                        Compat.playAt(anchor, "BLOCK_CHAIN_BREAK", 1f, 0.8f);
                    }
                }),

                // Six runes orbit and charge the crate with enchantment glyphs.
                new Simple("RUNES", "BLOCK_ENCHANTMENT_TABLE_USE", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    double progress = tick / (double) duration;
                    for (Vector p : Curves.ring(1.5 - progress * 0.9, 6, tick * 0.12)) {
                        Location at = anchor.clone().add(p).add(0, 0.4 + Math.sin(tick * 0.2) * 0.15, 0);
                        Compat.spawn(Compat.particle("WAX_ON"), at, 1, 0, 0, 0, 0);
                        Compat.spawn(Compat.particle("ENCHANT"), at, 3, 0.05, 0.1, 0.05, 0.4);
                    }
                    if (tick % 16 == 0) Compat.playAt(anchor, "BLOCK_AMETHYST_BLOCK_RESONATE", 0.7f, 0.9f);
                }),

                // A converging blizzard that freezes onto the crate.
                new Simple("SNOW_STORM", "ENTITY_PLAYER_HURT_FREEZE", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    long duration = ctx.crate().animation().preDelayTicks();
                    double progress = tick / (double) duration;
                    double radius = 2.4 * (1 - progress) + 0.2;
                    for (int i = 0; i < 3; i++) {
                        double a = tick * 0.3 + i * 2 * Math.PI / 3;
                        double y = 1.6 - ((tick * 0.05 + i * 0.5) % 1.6);
                        Compat.spawn(Compat.particle("SNOWFLAKE"),
                                anchor.clone().add(radius * Math.cos(a), y, radius * Math.sin(a)),
                                2, 0.05, 0.05, 0.05, 0.01);
                    }
                    if (tick % 12 == 0) Compat.playAt(anchor, "BLOCK_POWDER_SNOW_STEP", 0.9f, 0.7f);
                    if (tick + 2 >= duration) {
                        Compat.spawn(Compat.particle("ITEM_SNOWBALL"), anchor, 20, 0.5, 0.4, 0.5, 0.1);
                        Compat.playAt(anchor, "BLOCK_GLASS_BREAK", 0.8f, 1.4f);
                    }
                }),

                // Dragon's breath pours onto the crate from above.
                new Simple("DRAGON_BREATH", "ENTITY_ENDER_DRAGON_GROWL", (ctx, tick) -> {
                    Location anchor = ctx.anchor();
                    Location mouth = anchor.clone().add(0, 3.2, 0);
                    for (Location at : Curves.line(mouth, anchor, 8)) {
                        Compat.spawn(Compat.particle("DRAGON_BREATH"), at, 2, 0.15, 0.05, 0.15, 0.01);
                    }
                    for (Vector p : Curves.ring(0.9, 6, tick * 0.25)) {
                        Compat.spawn(Compat.particle("DRAGON_BREATH"),
                                anchor.clone().add(p).add(0, 0.15, 0), 1, 0.05, 0.02, 0.05, 0);
                    }
                    if (tick % 18 == 0) Compat.playAt(anchor, "ENTITY_ENDER_DRAGON_FLAP", 0.8f, 0.9f);
                })
        );
    }

    /** Shared plumbing: intro sound, tick body for the configured duration, done. */
    record Simple(String id, String introSound, BiConsumer<OpeningContext, Integer> body)
            implements PreOpenAnimation {

        @Override
        public void play(OpeningContext ctx) {
            Compat.playAt(ctx.anchor(), introSound, 0.9f, 1f);
            Ticker.run(ctx, ctx.crate().animation().preDelayTicks(), 2,
                    tick -> body.accept(ctx, tick));
        }
    }

    /** Convenience for future addons: a floating prop item over the crate. */
    static ItemDisplay prop(OpeningContext ctx, Material material, double y, float scale) {
        ItemDisplay display = Displays.item(ctx.anchor().clone().add(0, y, 0), new ItemStack(material), scale);
        ctx.onPhaseCleanup(display::remove);
        return display;
    }
}
