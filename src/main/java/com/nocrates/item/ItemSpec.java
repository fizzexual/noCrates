package com.nocrates.item;

import com.nocrates.compat.Compat;
import com.nocrates.compat.ItemModelCompat;
import com.nocrates.hook.CustomItems;
import com.nocrates.text.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Declarative, hand-editable item description used everywhere items appear in YAML
 * (keys, reward display/win items, menu icons, crate models). Parsing and writing are
 * pure (unit-testable); only {@link #build()} touches the Bukkit item factory.
 *
 * Fields: material, amount, name, lore, glow, custom-model-data, item-model,
 * head-texture (base64), enchants, flags, unbreakable, custom-item ("itemsadder:id",
 * "oraxen:id", "nexo:id", "mmoitems:TYPE:ID").
 */
public final class ItemSpec {

    private String material = "STONE";
    private int amount = 1;
    private String name;
    private List<String> lore = new ArrayList<>();
    private boolean glow;
    private int customModelData = -1;
    private String itemModel;
    private String headTexture;
    private Map<String, Integer> enchants = new LinkedHashMap<>();
    private List<String> flags = new ArrayList<>();
    private boolean unbreakable;
    private String customItem;

    public ItemSpec() {
    }

    public ItemSpec(String material) {
        this.material = material;
    }

    public static ItemSpec fromConfig(ConfigurationSection s) {
        ItemSpec spec = new ItemSpec();
        if (s == null) return spec;
        spec.material = s.getString("material", "STONE");
        spec.amount = Math.max(1, s.getInt("amount", 1));
        spec.name = s.getString("name");
        spec.lore = new ArrayList<>(s.getStringList("lore"));
        spec.glow = s.getBoolean("glow", false);
        spec.customModelData = s.getInt("custom-model-data", -1);
        spec.itemModel = s.getString("item-model");
        spec.headTexture = s.getString("head-texture");
        spec.unbreakable = s.getBoolean("unbreakable", false);
        spec.customItem = s.getString("custom-item");
        ConfigurationSection ench = s.getConfigurationSection("enchants");
        if (ench != null) {
            for (String key : ench.getKeys(false)) spec.enchants.put(key, ench.getInt(key, 1));
        }
        spec.flags = new ArrayList<>(s.getStringList("flags"));
        return spec;
    }

    public void write(ConfigurationSection s) {
        s.set("material", material);
        if (amount != 1) s.set("amount", amount);
        s.set("name", name);
        s.set("lore", lore.isEmpty() ? null : lore);
        if (glow) s.set("glow", true);
        if (customModelData >= 0) s.set("custom-model-data", customModelData);
        s.set("item-model", itemModel);
        s.set("head-texture", headTexture);
        if (unbreakable) s.set("unbreakable", true);
        s.set("custom-item", customItem);
        if (!enchants.isEmpty()) {
            for (Map.Entry<String, Integer> e : enchants.entrySet()) s.set("enchants." + e.getKey(), e.getValue());
        }
        if (!flags.isEmpty()) s.set("flags", flags);
    }

    /** Captures a runtime item into a spec (used by the editor for held items). */
    public static ItemSpec fromItem(ItemStack it) {
        ItemSpec spec = new ItemSpec();
        if (it == null) return spec;
        spec.material = it.getType().name();
        spec.amount = it.getAmount();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) spec.name = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(Objects.requireNonNull(meta.displayName()));
            if (meta.hasLore() && meta.lore() != null) {
                for (net.kyori.adventure.text.Component c : Objects.requireNonNull(meta.lore()))
                    spec.lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(c));
            }
            if (meta.hasCustomModelData()) spec.customModelData = meta.getCustomModelData();
            if (meta.isUnbreakable()) spec.unbreakable = true;
            meta.getEnchants().forEach((e, lvl) -> spec.enchants.put(e.getKey().getKey().toUpperCase(Locale.ROOT), lvl));
        }
        return spec;
    }

    /** Builds the ItemStack; custom-item references resolve through installed hooks first. */
    public ItemStack build() {
        if (customItem != null && !customItem.isEmpty()) {
            ItemStack hooked = CustomItems.resolve(customItem);
            if (hooked != null) {
                hooked.setAmount(amount);
                return decorate(hooked);
            }
        }
        Material mat = Compat.material(material, Material.STONE);
        ItemStack it = new ItemStack(mat, amount);
        if (headTexture != null && !headTexture.isEmpty() && mat == Material.PLAYER_HEAD) {
            Heads.applyTexture(it, headTexture);
        }
        return decorate(it);
    }

    private ItemStack decorate(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        if (name != null && !name.isEmpty()) {
            meta.displayName(Text.mm("<!italic>" + name));
        }
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> out = new ArrayList<>();
            for (String line : lore) out.add(Text.mm("<!italic>" + line));
            meta.lore(out);
        }
        if (customModelData >= 0) meta.setCustomModelData(customModelData);
        if (itemModel != null) ItemModelCompat.setItemModel(meta, itemModel);
        if (unbreakable) meta.setUnbreakable(true);
        for (Map.Entry<String, Integer> e : enchants.entrySet()) {
            Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(e.getKey().toLowerCase(Locale.ROOT)));
            if (ench != null) meta.addEnchant(ench, Math.max(1, e.getValue()), true);
        }
        for (String flag : flags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (glow && enchants.isEmpty()) {
            Enchantment lure = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("lure"));
            if (lure != null) {
                meta.addEnchant(lure, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        it.setItemMeta(meta);
        return it;
    }

    public ItemSpec copy() {
        ItemSpec c = new ItemSpec();
        c.material = material;
        c.amount = amount;
        c.name = name;
        c.lore = new ArrayList<>(lore);
        c.glow = glow;
        c.customModelData = customModelData;
        c.itemModel = itemModel;
        c.headTexture = headTexture;
        c.enchants = new LinkedHashMap<>(enchants);
        c.flags = new ArrayList<>(flags);
        c.unbreakable = unbreakable;
        c.customItem = customItem;
        return c;
    }

    // --- accessors used by editor/serializers ---

    public String material() {
        return material;
    }

    public ItemSpec material(String material) {
        this.material = material;
        return this;
    }

    public int amount() {
        return amount;
    }

    public ItemSpec amount(int amount) {
        this.amount = Math.max(1, amount);
        return this;
    }

    public String name() {
        return name;
    }

    public ItemSpec name(String name) {
        this.name = name;
        return this;
    }

    public List<String> lore() {
        return lore;
    }

    public ItemSpec lore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }

    public boolean glow() {
        return glow;
    }

    public ItemSpec glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public int customModelData() {
        return customModelData;
    }

    public ItemSpec customModelData(int cmd) {
        this.customModelData = cmd;
        return this;
    }

    public String itemModel() {
        return itemModel;
    }

    public String headTexture() {
        return headTexture;
    }

    public ItemSpec headTexture(String headTexture) {
        this.headTexture = headTexture;
        return this;
    }

    public String customItem() {
        return customItem;
    }

    public ItemSpec customItem(String customItem) {
        this.customItem = customItem;
        return this;
    }

    public Map<String, Integer> enchants() {
        return enchants;
    }

    public List<String> flags() {
        return flags;
    }

    public boolean unbreakable() {
        return unbreakable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemSpec other)) return false;
        return amount == other.amount && glow == other.glow
                && customModelData == other.customModelData && unbreakable == other.unbreakable
                && Objects.equals(material, other.material) && Objects.equals(name, other.name)
                && Objects.equals(lore, other.lore) && Objects.equals(itemModel, other.itemModel)
                && Objects.equals(headTexture, other.headTexture) && Objects.equals(enchants, other.enchants)
                && Objects.equals(flags, other.flags) && Objects.equals(customItem, other.customItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, amount, name, lore, glow, customModelData, itemModel,
                headTexture, enchants, flags, unbreakable, customItem);
    }
}
