package com.nocrates.reward;

import com.nocrates.item.ItemSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback granted when the rolled reward is blocked (restricted permission or a hit
 * win limit): an optional item and/or commands.
 */
public final class AlternativeReward {

    private boolean enabled;
    private ItemSpec item;
    private String displayName = "";
    private boolean virtualOnly;
    private boolean broadcast;
    private List<String> commands = new ArrayList<>();

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ItemSpec item() {
        return item;
    }

    public void item(ItemSpec item) {
        this.item = item;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName == null ? "" : displayName;
    }

    public boolean virtualOnly() {
        return virtualOnly;
    }

    public void virtualOnly(boolean virtualOnly) {
        this.virtualOnly = virtualOnly;
    }

    public boolean broadcast() {
        return broadcast;
    }

    public void broadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public List<String> commands() {
        return commands;
    }

    public void commands(List<String> commands) {
        this.commands = new ArrayList<>(commands);
    }
}
