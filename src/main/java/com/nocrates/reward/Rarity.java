package com.nocrates.reward;

/**
 * A reward tier. {@code color} is a MiniMessage prefix (e.g. {@code <aqua>} or a
 * gradient) used to colour reward names consistently in previews, animations and
 * broadcasts. {@code broadcast} marks tiers worth announcing server-wide.
 */
public final class Rarity {

    private final String id;
    private final String color;
    private final int order;
    private final boolean broadcast;

    public Rarity(String id, String color, int order, boolean broadcast) {
        this.id = id;
        this.color = color;
        this.order = order;
        this.broadcast = broadcast;
    }

    public String id() {
        return id;
    }

    public String color() {
        return color;
    }

    public int order() {
        return order;
    }

    public boolean broadcast() {
        return broadcast;
    }

    /** Wrap text in this rarity's MiniMessage colour. */
    public String wrap(String text) {
        return color + (text == null ? "" : text);
    }
}
