package com.nocrates.reward;

/** Max wins (-1 = unlimited) plus an optional cooldown applied once the limit is hit. */
public record WinLimit(int max, long cooldownSeconds) {

    private static final WinLimit NONE = new WinLimit(-1, 0);

    public static WinLimit none() {
        return NONE;
    }

    public boolean unlimited() {
        return max < 0;
    }

    /** Whether another win is allowed given the current count and cooldown expiry. */
    public boolean allows(int currentWins, long cooldownUntilEpochSec, long nowEpochSec) {
        if (nowEpochSec < cooldownUntilEpochSec) return false;
        if (unlimited()) return true;
        return currentWins < max;
    }
}
