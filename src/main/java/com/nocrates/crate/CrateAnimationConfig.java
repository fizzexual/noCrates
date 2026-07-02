package com.nocrates.crate;

import java.util.ArrayList;
import java.util.List;

/**
 * The crate's animation selection: always-on idle effect specs plus one animation id
 * per opening phase (pre-open, post-open, reward-display) with per-phase delays.
 */
public final class CrateAnimationConfig {

    private List<String> idleEffects = new ArrayList<>();
    private String preOpen = "DEFAULT";
    private String postOpen = "BALL";
    private String rewardDisplay = "DEFAULT";
    private int preDelayTicks = 30;
    private int postDelayTicks = 30;
    private int displayDurationTicks = 60;

    public List<String> idleEffects() {
        return idleEffects;
    }

    public void idleEffects(List<String> idleEffects) {
        this.idleEffects = new ArrayList<>(idleEffects);
    }

    public String preOpen() {
        return preOpen;
    }

    public void preOpen(String preOpen) {
        this.preOpen = preOpen;
    }

    public String postOpen() {
        return postOpen;
    }

    public void postOpen(String postOpen) {
        this.postOpen = postOpen;
    }

    public String rewardDisplay() {
        return rewardDisplay;
    }

    public void rewardDisplay(String rewardDisplay) {
        this.rewardDisplay = rewardDisplay;
    }

    public int preDelayTicks() {
        return preDelayTicks;
    }

    public void preDelayTicks(int preDelayTicks) {
        this.preDelayTicks = Math.max(0, preDelayTicks);
    }

    public int postDelayTicks() {
        return postDelayTicks;
    }

    public void postDelayTicks(int postDelayTicks) {
        this.postDelayTicks = Math.max(0, postDelayTicks);
    }

    public int displayDurationTicks() {
        return displayDurationTicks;
    }

    public void displayDurationTicks(int displayDurationTicks) {
        this.displayDurationTicks = Math.max(20, displayDurationTicks);
    }
}
