package com.nocrates.util;

import com.nocrates.compat.VersionCompat;
import com.nocrates.reward.DisplaySpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Builds {@link ItemStack}s from specs and MiniMessage strings. */
public final class Items {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Items() {
    }

    public static ItemStack build(DisplaySpec spec) {
        Material material = VersionCompat.material(spec.material(), Material.STONE);
        ItemStack item = new ItemStack(material, Math.max(1, spec.amount()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (spec.name() != null && !spec.name().isBlank()) {
                meta.displayName(mini(spec.name()));
            }
            if (!spec.lore().isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : spec.lore()) {
                    lore.add(mini(line));
                }
                meta.lore(lore);
            }
            if (spec.glow()) {
                applyGlow(meta);
            }
            if (spec.customModelData() != null) {
                meta.setCustomModelData(spec.customModelData());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Quick icon with a MiniMessage name and optional lore lines. */
    public static ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(mini(name));
            }
            if (lore != null && lore.length > 0) {
                List<Component> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(mini(line));
                }
                meta.lore(lines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Deserialize MiniMessage and strip the default italic so items look clean. */
    public static Component mini(String text) {
        return MM.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    private static void applyGlow(ItemMeta meta) {
        try {
            Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft("infinity"));
            if (enchant != null) {
                meta.addEnchant(enchant, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } catch (Throwable ignored) {
            // enchantment registry differs on this version — skip the glow
        }
    }
}
