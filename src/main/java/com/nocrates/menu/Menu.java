package com.nocrates.menu;

import com.nocrates.compat.Compat;
import com.nocrates.item.ItemBuilder;
import com.nocrates.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Base inventory menu. Click routing runs through {@link InventoryHolder} so no
 * InventoryView methods are ever referenced (they broke binary compatibility in 1.21).
 */
public abstract class Menu implements InventoryHolder {

    protected final Player viewer;
    private final Inventory inventory;
    private final Map<Integer, MenuItem> items = new HashMap<>();

    protected Menu(Player viewer, String miniMessageTitle, int rows) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * 9, Text.mm(miniMessageTitle));
    }

    protected Menu(Player viewer, Component title, int rows) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * 9, title);
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    public final Player viewer() {
        return viewer;
    }

    /** (Re)draws content; called before opening and on refresh(). */
    protected abstract void draw();

    public final void open() {
        draw();
        viewer.openInventory(inventory);
    }

    public final void refresh() {
        inventory.clear();
        items.clear();
        draw();
    }

    protected final void set(int slot, MenuItem item) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        items.put(slot, item);
        inventory.setItem(slot, item.icon());
    }

    protected final void fillBorder(Material material) {
        var pane = new MenuItem(ItemBuilder.of(material).name(" ").build());
        int size = inventory.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                if (!items.containsKey(i)) set(i, pane);
            }
        }
    }

    protected final void clickSound() {
        Compat.play(viewer, "UI_BUTTON_CLICK", 0.6f, 1.2f);
    }

    /** Routed by MenuListener for clicks in the top inventory. */
    final void handleClick(InventoryClickEvent event) {
        MenuItem item = items.get(event.getSlot());
        if (item != null) item.click(event);
    }

    /** Clicks on the viewer's own inventory while this menu is open; return true to cancel. */
    protected boolean clickBottom(InventoryClickEvent event) {
        return true;
    }

    protected void onClose() {
    }

    final boolean bottomClick(InventoryClickEvent event) {
        return clickBottom(event);
    }

    final void closed() {
        onClose();
    }
}
