package com.nocrates.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weighted random selection over a crate's rewards. A reward's {@code weight}
 * is its relative chance; the engine normalises across all positive weights.
 * The {@link Random} is injected so the distribution is deterministically
 * testable.
 */
public final class RollEngine {

    private final Random random;

    public RollEngine(Random random) {
        this.random = random;
    }

    /** Pick one reward weighted by {@link Reward#weight()}. */
    public Reward roll(List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return null;
        }
        double total = 0;
        for (Reward reward : rewards) {
            if (reward.weight() > 0) {
                total += reward.weight();
            }
        }
        if (total <= 0) {
            return rewards.get(rewards.size() - 1);
        }
        double cursor = random.nextDouble() * total;
        for (Reward reward : rewards) {
            if (reward.weight() <= 0) {
                continue;
            }
            cursor -= reward.weight();
            if (cursor < 0) {
                return reward;
            }
        }
        for (int i = rewards.size() - 1; i >= 0; i--) {
            if (rewards.get(i).weight() > 0) {
                return rewards.get(i);
            }
        }
        return null;
    }

    /** Roll among rewards of the given rarity only; falls back to a full roll. */
    public Reward rollTier(List<Reward> rewards, String rarityId) {
        if (rarityId == null) {
            return roll(rewards);
        }
        List<Reward> filtered = new ArrayList<>();
        for (Reward reward : rewards) {
            if (rarityId.equalsIgnoreCase(reward.rarityId())) {
                filtered.add(reward);
            }
        }
        return filtered.isEmpty() ? roll(rewards) : roll(filtered);
    }
}
