package com.nocrates.animation;

import com.nocrates.reward.DisplaySpec;
import com.nocrates.reward.Reward;
import com.nocrates.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Shared helpers for building reward icons and checking viewer state. */
public final class Animations {

    private Animations() {
    }

    public static ItemStack icon(Reward reward) {
        if (reward == null) {
            return new ItemStack(Material.BARRIER);
        }
        return reward.display() != null
                ? Items.build(reward.display())
                : Items.icon(Material.CHEST, "<white>" + reward.id());
    }

    /** Same icon, but forced to glow (used to highlight the won reward). */
    public static ItemStack glow(Reward reward) {
        DisplaySpec display = reward == null ? null : reward.display();
        if (display == null) {
            return icon(reward);
        }
        if (display.glow()) {
            return Items.build(display);
        }
        return Items.build(new DisplaySpec(display.material(), display.name(), display.lore(),
                display.amount(), true, display.customModelData()));
    }

    public static boolean isViewing(Player player, Inventory inventory) {
        return inventory != null
                && player.getOpenInventory() != null
                && inventory.equals(player.getOpenInventory().getTopInventory());
    }
}
