package com.nocrates.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Routes inventory events to {@link Menu} holders. */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) return;
        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == event.getInventory()) {
            event.setCancelled(true);
            menu.handleClick(event);
        } else {
            // Click in the player's own inventory while a menu is open.
            if (event.isShiftClick() || menu.bottomClick(event)) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        int topSize = event.getInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu menu) menu.closed();
    }
}
