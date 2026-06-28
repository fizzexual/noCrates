package com.nocrates.reward;

import java.util.List;

/**
 * Pure description of how a reward is shown in menus and animations. Kept free
 * of Bukkit types so crate (de)serialization is unit-testable without a server;
 * {@link com.nocrates.util.Items#build(DisplaySpec)} turns it into an ItemStack
 * at runtime.
 */
public record DisplaySpec(String material, String name, List<String> lore, int amount, boolean glow,
                          Integer customModelData) {

    public DisplaySpec {
        if (lore == null) {
            lore = List.of();
        }
        if (amount <= 0) {
            amount = 1;
        }
    }

    public static DisplaySpec of(String material, String name) {
        return new DisplaySpec(material, name, List.of(), 1, false, null);
    }
}
