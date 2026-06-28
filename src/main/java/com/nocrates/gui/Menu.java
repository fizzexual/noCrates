package com.nocrates.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for chest-style menus. A {@code Menu} is its own
 * {@link InventoryHolder}, so {@link MenuListener} can route clicks back to it.
 * Subclasses implement {@link #build(Player)} to lay out icons.
 */
public abstract class Menu implements InventoryHolder {

    private final Map<Integer, MenuItem> items = new HashMap<>();
    private final int size;
    private final Component title;
    private Inventory inventory;

    protected Menu(int size, Component title) {
        this.size = size;
        this.title = title;
    }

    /** Populate the menu via {@link #set} calls. Called fresh on every open. */
    protected abstract void build(Player player);

    protected void set(int slot, MenuItem item) {
        if (slot < 0 || slot >= size) {
            return;
        }
        items.put(slot, item);
        if (inventory != null) {
            inventory.setItem(slot, item == null ? null : item.icon());
        }
    }

    protected void set(int slot, ItemStack icon, Consumer<InventoryClickEvent> onClick) {
        set(slot, new MenuItem(icon, onClick));
    }

    protected void fill(ItemStack icon) {
        for (int slot = 0; slot < size; slot++) {
            if (!items.containsKey(slot)) {
                set(slot, MenuItem.display(icon));
            }
        }
    }

    public void open(Player player) {
        this.inventory = Bukkit.createInventory(this, size, title);
        items.clear();
        build(player);
        player.openInventory(inventory);
    }

    /** Re-run {@link #build(Player)} in place without reopening the window. */
    public void refresh(Player player) {
        if (inventory == null) {
            open(player);
            return;
        }
        items.clear();
        inventory.clear();
        build(player);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        MenuItem item = items.get(event.getRawSlot());
        if (item != null) {
            item.click(event);
        }
    }

    protected int size() {
        return size;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
