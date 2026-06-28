package com.nocrates.key;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.reward.DisplaySpec;
import com.nocrates.storage.PlayerDataManager;
import com.nocrates.util.Items;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Manages crate keys in both forms: virtual balances (via {@link PlayerDataManager})
 * and physical items tagged with a {@link PersistentDataContainer} key so they
 * survive renames and stacking.
 */
public final class KeyManager {

    private final PlayerDataManager data;
    private final NamespacedKey keyTag;

    public KeyManager(NoCrates plugin, PlayerDataManager data) {
        this.data = data;
        this.keyTag = new NamespacedKey(plugin, "crate_key");
    }

    // ---- virtual keys --------------------------------------------------

    public int virtual(UUID id, Crate crate) {
        return data.get(id).keys(crate.name(), crate.key().keyId());
    }

    public void giveVirtual(UUID id, Crate crate, int amount) {
        data.get(id).addKeys(crate.name(), crate.key().keyId(), amount);
    }

    public boolean takeVirtual(UUID id, Crate crate, int amount) {
        return data.get(id).takeKeys(crate.name(), crate.key().keyId(), amount);
    }

    // ---- physical keys -------------------------------------------------

    public ItemStack physicalKey(Crate crate, int amount) {
        DisplaySpec spec = crate.key().item();
        ItemStack item = spec != null
                ? Items.build(spec)
                : Items.icon(Material.TRIPWIRE_HOOK, "<yellow>" + crate.name() + " Key");
        item.setAmount(Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, crate.name().toLowerCase());
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isPhysicalKey(ItemStack item, Crate crate) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String tag = item.getItemMeta().getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
        return tag != null && tag.equalsIgnoreCase(crate.name());
    }

    public int countPhysical(Player player, Crate crate) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isPhysicalKey(item, crate)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public boolean consumePhysical(Player player, Crate crate, int amount) {
        if (countPhysical(player, crate) < amount) {
            return false;
        }
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (isPhysicalKey(item, crate)) {
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        return true;
    }

    // ---- combined ------------------------------------------------------

    /** Total keys held (virtual + physical) respecting the crate's key type. */
    public int total(Player player, Crate crate) {
        KeyType type = crate.key().type();
        int count = 0;
        if (type.allowsVirtual()) {
            count += virtual(player.getUniqueId(), crate);
        }
        if (type.allowsPhysical()) {
            count += countPhysical(player, crate);
        }
        return count;
    }

    public boolean has(Player player, Crate crate) {
        return !crate.key().type().requiresKey() || total(player, crate) > 0;
    }

    /** Consume a single key, preferring a physical key when one is held. */
    public boolean consumeOne(Player player, Crate crate) {
        KeyType type = crate.key().type();
        if (!type.requiresKey()) {
            return true; // lootbox — nothing to consume
        }
        if (type.allowsPhysical() && countPhysical(player, crate) > 0) {
            return consumePhysical(player, crate, 1);
        }
        if (type.allowsVirtual() && virtual(player.getUniqueId(), crate) > 0) {
            return takeVirtual(player.getUniqueId(), crate, 1);
        }
        return false;
    }

    /** Give one key back — used to refund an aborted open. */
    public void refundOne(Player player, Crate crate) {
        KeyType type = crate.key().type();
        if (!type.requiresKey()) {
            return;
        }
        if (type.allowsVirtual()) {
            giveVirtual(player.getUniqueId(), crate, 1);
        } else if (type.allowsPhysical()) {
            player.getInventory().addItem(physicalKey(crate, 1)).values()
                    .forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
        }
    }
}
