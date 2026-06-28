package com.nocrates.open;

import com.nocrates.animation.Animations;
import com.nocrates.crate.Crate;
import com.nocrates.gui.Menu;
import com.nocrates.reward.Rarity;
import com.nocrates.reward.RarityRegistry;
import com.nocrates.reward.Reward;
import com.nocrates.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only menu listing every reward with its real (normalised) chance. */
public final class PreviewMenu extends Menu {

    private final Crate crate;
    private final RarityRegistry rarities;

    public PreviewMenu(Crate crate, RarityRegistry rarities) {
        super(size(crate), Items.mini(crate.previewTitle()));
        this.crate = crate;
        this.rarities = rarities;
    }

    private static int size(Crate crate) {
        int rows = Math.max(1, (int) Math.ceil(crate.rewards().size() / 9.0));
        return Math.min(6, rows) * 9;
    }

    @Override
    protected void build(Player player) {
        double total = 0;
        for (Reward reward : crate.rewards()) {
            if (reward.weight() > 0) {
                total += reward.weight();
            }
        }
        int slot = 0;
        for (Reward reward : crate.rewards()) {
            if (slot >= size()) {
                break;
            }
            double pct = (total > 0 && reward.weight() > 0) ? reward.weight() / total * 100.0 : 0;
            set(slot, withChance(reward, rarities.get(reward.rarityId()), pct), event -> {
            });
            slot++;
        }
    }

    private ItemStack withChance(Reward reward, Rarity rarity, double pct) {
        ItemStack icon = Animations.icon(reward);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Items.mini("<dark_gray>──────────────"));
            lore.add(Items.mini("<gray>Rarity: " + rarity.wrap(capitalize(reward.rarityId()))));
            lore.add(Items.mini("<gray>Chance: <white>" + format(pct) + "%"));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String format(double pct) {
        return (pct == Math.floor(pct)) ? String.valueOf((long) pct) : String.format(Locale.US, "%.2f", pct);
    }
}
