package com.nocrates.crate;

import com.nocrates.core.Services;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.nocrates.open.PreviewMenu;

/**
 * Right-click a crate = open (sneak = quick-open), left-click = preview. Also turns
 * placed "crate items" (/crates givecrate) into live placements.
 */
public final class CrateClickListener implements Listener {

    public static final NamespacedKey PDC_CRATE_ITEM = NamespacedKey.fromString("nocrates:crateitem");

    private final PlacementManager placements;

    public CrateClickListener(PlacementManager placements) {
        this.placements = placements;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        CratePlacement placement = placements.at(event.getClickedBlock());
        if (placement == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Crate crate = placement.crate();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Services.get().openService().attempt(player, crate, placement, player.isSneaking());
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (crate.previewEnabled()) new PreviewMenu(player, crate).open();
        }
    }

    /** Placing a tagged crate item binds the crate to the placed block. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String crateId = meta.getPersistentDataContainer().get(PDC_CRATE_ITEM, PersistentDataType.STRING);
        if (crateId == null) return;
        Crate crate = Services.get().crates().get(crateId);
        if (crate == null) return;
        placements.attach(crate, event.getBlockPlaced());
    }

    /** Builds the placeable crate item for /crates givecrate. */
    public static ItemStack crateItem(Crate crate, int amount) {
        ItemStack item;
        if (crate.engine() == Crate.EngineType.MODEL) {
            item = crate.modelItem().build();
        } else {
            var material = com.nocrates.compat.Compat.material(crate.blockMaterial(), org.bukkit.Material.CHEST);
            item = com.nocrates.item.ItemBuilder.of(material).name(crate.displayName()).build();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(PDC_CRATE_ITEM, PersistentDataType.STRING, crate.id());
            item.setItemMeta(meta);
        }
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return item;
    }
}
