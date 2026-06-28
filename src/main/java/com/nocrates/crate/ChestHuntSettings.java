package com.nocrates.crate;

/**
 * Tuning for the {@code chesthunt} animation: how many chests spawn, how many
 * the player may open, the search radius around them, and the timeout.
 */
public record ChestHuntSettings(int chests, int picks, int radius, int timeoutSeconds) {

    public ChestHuntSettings {
        chests = Math.max(1, chests);
        picks = Math.max(1, Math.min(picks, chests));
        radius = Math.max(1, radius);
        timeoutSeconds = Math.max(5, timeoutSeconds);
    }

    public static ChestHuntSettings defaults() {
        return new ChestHuntSettings(8, 4, 2, 30);
    }
}
