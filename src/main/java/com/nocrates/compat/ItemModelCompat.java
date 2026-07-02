package com.nocrates.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

/**
 * Applies the {@code minecraft:item_model} component (ItemMeta#setItemModel, added in
 * 1.21.2) when the server supports it. Called reflectively because our compile target
 * is the 1.20.1 API.
 */
public final class ItemModelCompat {

    private static final Method SET_ITEM_MODEL = lookup();

    private ItemModelCompat() {
    }

    private static Method lookup() {
        try {
            return ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static boolean supported() {
        return SET_ITEM_MODEL != null;
    }

    /** Returns true when applied; false (with no side effect) on servers older than 1.21.2. */
    public static boolean setItemModel(ItemMeta meta, String key) {
        if (SET_ITEM_MODEL == null || meta == null || key == null || key.isEmpty()) return false;
        NamespacedKey nk = NamespacedKey.fromString(key.toLowerCase(java.util.Locale.ROOT));
        if (nk == null) return false;
        try {
            SET_ITEM_MODEL.invoke(meta, nk);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
