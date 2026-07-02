package com.nocrates.item;

import com.nocrates.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Fluent runtime item construction for menus, editor icons and keys. */
public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack.clone());
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        return edit(m -> m.displayName(Text.mm("<!italic>" + miniMessage)));
    }

    public ItemBuilder name(Component name) {
        return edit(m -> m.displayName(name));
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) lore.add(Text.mm("<!italic>" + line));
        return edit(m -> m.lore(lore));
    }

    public ItemBuilder loreComponents(List<Component> lines) {
        return edit(m -> m.lore(lines));
    }

    public ItemBuilder glow(boolean glow) {
        if (!glow) return this;
        return edit(m -> {
            Enchantment lure = Enchantment.getByKey(NamespacedKey.minecraft("lure"));
            if (lure != null) {
                m.addEnchant(lure, 1, true);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        });
    }

    public ItemBuilder modelData(int cmd) {
        if (cmd >= 0) edit(m -> m.setCustomModelData(cmd));
        return this;
    }

    public ItemBuilder hideAttributes() {
        return edit(m -> m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS));
    }

    public ItemBuilder pdc(NamespacedKey key, String value) {
        return edit(m -> m.getPersistentDataContainer().set(key, PersistentDataType.STRING, value));
    }

    public ItemBuilder edit(java.util.function.Consumer<ItemMeta> fn) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            fn.accept(meta);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }
}
