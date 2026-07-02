package com.nocrates.reward;

import com.nocrates.item.ItemSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * One crate reward. The display item is what previews/animations show; win-items and
 * win-commands are what the player actually receives. Percentage is a relative weight —
 * real chances are normalized across the crate.
 */
public final class Reward {

    private final String id;
    private double percentage = 10.0;
    private ItemSpec displayItem = new ItemSpec("CHEST");
    private List<ItemSpec> winItems = new ArrayList<>();
    private List<String> winCommands = new ArrayList<>();
    private boolean broadcast;
    private boolean virtualReward;
    private boolean shareOnline;
    private List<String> restrictedPermissions = new ArrayList<>();
    private WinLimit playerLimit = WinLimit.none();
    private WinLimit globalLimit = WinLimit.none();
    private AlternativeReward alternative = new AlternativeReward();
    private String rarity;
    /** In SELECTIVE mode: how many linked-key units this choice costs. */
    private int selectiveCost = 1;
    /** Granted on EVERY opening (lootbox "guaranteed items"); excluded from the roll pool. */
    private boolean always;

    public Reward(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public double percentage() {
        return percentage;
    }

    public void percentage(double percentage) {
        this.percentage = Math.max(0, percentage);
    }

    public ItemSpec displayItem() {
        return displayItem;
    }

    public void displayItem(ItemSpec displayItem) {
        this.displayItem = displayItem;
    }

    public List<ItemSpec> winItems() {
        return winItems;
    }

    public List<String> winCommands() {
        return winCommands;
    }

    public boolean broadcast() {
        return broadcast;
    }

    public void broadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public boolean virtualReward() {
        return virtualReward;
    }

    public void virtualReward(boolean virtualReward) {
        this.virtualReward = virtualReward;
    }

    public boolean shareOnline() {
        return shareOnline;
    }

    public void shareOnline(boolean shareOnline) {
        this.shareOnline = shareOnline;
    }

    public List<String> restrictedPermissions() {
        return restrictedPermissions;
    }

    public WinLimit playerLimit() {
        return playerLimit;
    }

    public void playerLimit(WinLimit playerLimit) {
        this.playerLimit = playerLimit;
    }

    public WinLimit globalLimit() {
        return globalLimit;
    }

    public void globalLimit(WinLimit globalLimit) {
        this.globalLimit = globalLimit;
    }

    public AlternativeReward alternative() {
        return alternative;
    }

    public void alternative(AlternativeReward alternative) {
        this.alternative = alternative;
    }

    public String rarity() {
        return rarity;
    }

    public void rarity(String rarity) {
        this.rarity = rarity;
    }

    public int selectiveCost() {
        return selectiveCost;
    }

    public void selectiveCost(int selectiveCost) {
        this.selectiveCost = Math.max(1, selectiveCost);
    }

    public boolean always() {
        return always;
    }

    public void always(boolean always) {
        this.always = always;
    }

    /** Display name for chat: explicit display-item name, else the material. */
    public String displayName() {
        if (displayItem.name() != null && !displayItem.name().isEmpty()) return displayItem.name();
        return displayItem.material();
    }
}
