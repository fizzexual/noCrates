package com.nocrates.reward;

import java.util.List;

/**
 * A single possible crate outcome. Pure data: {@code actions} are stored as raw
 * DSL lines (e.g. {@code "item: DIAMOND 5"}) and only parsed into executable
 * {@link RewardAction}s at grant time, so this class stays free of Bukkit types
 * and fully unit-testable.
 */
public final class Reward {

    private final String id;
    private final String rarityId;
    private final double weight;
    private final DisplaySpec display;
    private final List<String> actions;
    private final int maxPerPlayer;
    private final int cooldownSeconds;

    public Reward(String id, String rarityId, double weight, DisplaySpec display,
                  List<String> actions, int maxPerPlayer, int cooldownSeconds) {
        this.id = id;
        this.rarityId = rarityId == null ? "common" : rarityId;
        this.weight = weight;
        this.display = display;
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.maxPerPlayer = maxPerPlayer;
        this.cooldownSeconds = cooldownSeconds;
    }

    /** Minimal reward for tests / programmatic use. */
    public static Reward of(String id, double weight) {
        return new Reward(id, "common", weight, null, List.of(), -1, 0);
    }

    public String id() {
        return id;
    }

    public String rarityId() {
        return rarityId;
    }

    public double weight() {
        return weight;
    }

    public DisplaySpec display() {
        return display;
    }

    public List<String> actions() {
        return actions;
    }

    public int maxPerPlayer() {
        return maxPerPlayer;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }
}
