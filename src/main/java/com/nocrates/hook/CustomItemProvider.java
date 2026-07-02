package com.nocrates.hook;

import org.bukkit.inventory.ItemStack;

/** Bridge to a custom-item plugin (ItemsAdder, Oraxen, Nexo, MMOItems, addons...). */
public interface CustomItemProvider {

    /** Namespace this provider answers for, e.g. "itemsadder". */
    String namespace();

    boolean available();

    /** Resolves an item id (the part after the namespace) or returns null. */
    ItemStack resolve(String id);
}
