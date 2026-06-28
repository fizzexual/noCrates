package com.nocrates.reward;

/**
 * Milestone / "pity" rule for a crate: every {@code every} opens, the next open
 * is forced to a reward of {@code tier} rarity. Pure logic so it can be tested
 * without a server.
 */
public final class Pity {

    private final boolean enabled;
    private final int every;
    private final String tier;

    public Pity(boolean enabled, int every, String tier) {
        this.enabled = enabled;
        this.every = every;
        this.tier = tier;
    }

    public static Pity disabled() {
        return new Pity(false, 0, null);
    }

    /**
     * @param priorOpens how many times this player has already opened the crate
     * @return true if the open happening now (number {@code priorOpens + 1})
     *         lands on the milestone and should be forced
     */
    public boolean shouldForce(int priorOpens) {
        return enabled && every > 0 && ((priorOpens + 1) % every == 0);
    }

    public boolean enabled() {
        return enabled;
    }

    public int every() {
        return every;
    }

    public String tier() {
        return tier;
    }
}
