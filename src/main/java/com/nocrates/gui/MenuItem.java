package com.nocrates.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/** An icon plus its click behaviour within a {@link Menu}. */
public final class MenuItem {

    private final ItemStack icon;
    private final Consumer<InventoryClickEvent> onClick;

    public MenuItem(ItemStack icon, Consumer<InventoryClickEvent> onClick) {
        this.icon = icon;
        this.onClick = onClick;
    }

    public static MenuItem display(ItemStack icon) {
        return new MenuItem(icon, null);
    }

    public ItemStack icon() {
        return icon;
    }

    public void click(InventoryClickEvent event) {
        if (onClick != null) {
            onClick.accept(event);
        }
    }
}
