package com.nocrates.crate;

/** Per-crate opening behaviour. */
public final class OpenSettings {

    private double cost;
    private int cooldownSeconds;
    private boolean quickOpen = true;
    private boolean simultaneous = true;
    private boolean knockback = true;

    /** Vault money charged per open (0 = free). */
    public double cost() {
        return cost;
    }

    public void cost(double cost) {
        this.cost = Math.max(0, cost);
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public void cooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    /** Allow sneak+right-click to skip the animation. */
    public boolean quickOpen() {
        return quickOpen;
    }

    public void quickOpen(boolean quickOpen) {
        this.quickOpen = quickOpen;
    }

    /** Allow several players to open this crate('s placement) at the same time. */
    public boolean simultaneous() {
        return simultaneous;
    }

    public void simultaneous(boolean simultaneous) {
        this.simultaneous = simultaneous;
    }

    /** Knock players back when they try to open without a key. */
    public boolean knockback() {
        return knockback;
    }

    public void knockback(boolean knockback) {
        this.knockback = knockback;
    }
}
