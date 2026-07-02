package com.nocrates.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/** Weighted random selection; seedable for deterministic tests. */
public final class RollEngine {

    private final Random rng;

    public RollEngine(Random rng) {
        this.rng = rng;
    }

    /** Rolls one reward by weight; zero-weight rewards can never be selected. */
    public Reward roll(List<Reward> pool, ToDoubleFunction<Reward> weight) {
        double total = 0;
        for (Reward r : pool) total += Math.max(0, weight.applyAsDouble(r));
        if (total <= 0) return null;
        double pick = rng.nextDouble() * total;
        double running = 0;
        for (Reward r : pool) {
            double w = Math.max(0, weight.applyAsDouble(r));
            if (w <= 0) continue;
            running += w;
            if (pick < running) return r;
        }
        // Floating point edge: return the last weighted reward.
        for (int i = pool.size() - 1; i >= 0; i--) {
            if (weight.applyAsDouble(pool.get(i)) > 0) return pool.get(i);
        }
        return null;
    }

    public Reward roll(List<Reward> pool) {
        return roll(pool, Reward::percentage);
    }

    /**
     * Rolls {@code n} rewards. Each roll draws from the remaining pool (no duplicates)
     * until the pool is exhausted, after which duplicates are allowed — so a crate with
     * fewer rewards than max-win-rewards still fills every slot.
     */
    public List<Reward> rollN(List<Reward> pool, int n, ToDoubleFunction<Reward> weight) {
        List<Reward> out = new ArrayList<>();
        List<Reward> remaining = new ArrayList<>(pool);
        for (int i = 0; i < n; i++) {
            List<Reward> source = remaining.isEmpty() ? pool : remaining;
            Reward rolled = roll(source, weight);
            if (rolled == null) break;
            out.add(rolled);
            remaining.remove(rolled);
        }
        return out;
    }
}
