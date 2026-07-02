package com.nocrates.key;

import com.nocrates.item.ItemSpec;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * A first-class key definition from keys.yml. Keys are linked to crates from the crate
 * side ({@link KeyLink}), so one key can open many crates and one crate can accept
 * several keys with different priorities.
 */
public final class Key {

    public static final NamespacedKey PDC_KEY = NamespacedKey.fromString("nocrates:key");

    private final String id;
    private ItemSpec item;
    private boolean virtual;

    public Key(String id, ItemSpec item, boolean virtual) {
        this.id = id;
        this.item = item;
        this.virtual = virtual;
    }

    public String id() {
        return id;
    }

    public ItemSpec item() {
        return item;
    }

    public void item(ItemSpec item) {
        this.item = item;
    }

    /** Virtual-only keys never exist as items (prevents duping); physical drops are disabled. */
    public boolean virtual() {
        return virtual;
    }

    public void virtual(boolean virtual) {
        this.virtual = virtual;
    }

    /** Builds the tagged physical key item. */
    public ItemStack physical(int amount) {
        ItemStack stack = item.build();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, id);
            stack.setItemMeta(meta);
        }
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return stack;
    }
}
