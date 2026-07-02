package com.nocrates.reward;

import java.util.function.ToDoubleFunction;

/**
 * Pluggable reward weight resolution. The rarities module replaces the resolver to
 * synchronize weights across a rarity tier; everything (rolls, previews) reads through
 * here so displayed chances always match real odds.
 */
public final class Weights {

    private static volatile ToDoubleFunction<Reward> resolver = Reward::percentage;

    private Weights() {
    }

    public static void resolver(ToDoubleFunction<Reward> fn) {
        resolver = fn != null ? fn : Reward::percentage;
    }

    public static double of(Reward reward) {
        return Math.max(0, resolver.applyAsDouble(reward));
    }
}
