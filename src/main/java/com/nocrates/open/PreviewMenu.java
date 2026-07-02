package com.nocrates.open;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemBuilder;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuConfig;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.Reward;
import com.nocrates.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Paged reward preview. Shows both the configured weight ("percentage") and the
 * normalized real chance, PhoenixCrates-style. Layout comes from menus/preview.yml.
 */
public final class PreviewMenu extends Menu {

    private final Crate crate;
    private final MenuConfig.Spec spec;
    private int page;

    public PreviewMenu(Player viewer, Crate crate) {
        super(viewer, title(crate), Services.get().menus().get(crate.previewMenu()).rows());
        this.crate = crate;
        this.spec = Services.get().menus().get(crate.previewMenu());
    }

    private static String title(Crate crate) {
        return Services.get().menus().get(crate.previewMenu()).title().replace("<crate>", crate.displayName());
    }

    @Override
    protected void draw() {
        List<Integer> slots = contentSlots();
        List<Reward> rewards = crate.rewardList();
        int perPage = slots.size();
        int pages = Math.max(1, (rewards.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);

        for (int i = 0; i < perPage; i++) {
            int index = page * perPage + i;
            if (index >= rewards.size()) break;
            set(slots.get(i), new MenuItem(rewardIcon(rewards.get(index))));
        }
        navIcon("previous", page > 0, () -> {
            page--;
            refresh();
        });
        navIcon("next", page < pages - 1, () -> {
            page++;
            refresh();
        });
        navIcon("close", true, () -> viewer.closeInventory());
    }

    private ItemStack rewardIcon(Reward reward) {
        ItemStack icon = reward.displayItem().build();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() && meta.lore() != null
                    ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            var lang = Services.get().lang();
            lore.add(Component.empty());
            String rarity = com.nocrates.reward.RarityDisplays.get(reward.rarity());
            if (rarity != null) lore.add(Text.mm(rarity));
            lore.add(Text.mm(lang.rawString("preview-lore-weight")
                    .replace("<weight>", trim(com.nocrates.reward.Weights.of(reward)))));
            lore.add(Text.mm(lang.rawString("preview-lore-chance")
                    .replace("<chance>", trim(crate.normalizedChance(reward)))));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private List<Integer> contentSlots() {
        if (!spec.contentSlots().isEmpty()) return spec.contentSlots();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < (spec.rows() - 1) * 9; i++) slots.add(i);
        return slots;
    }

    private void navIcon(String key, boolean active, Runnable action) {
        MenuConfig.Icon icon = spec.icon(key);
        if (icon == null) return;
        for (int slot : icon.slots()) {
            if (!active) {
                set(slot, new MenuItem(ItemBuilder.of(org.bukkit.Material.GRAY_STAINED_GLASS_PANE).name(" ").build()));
                continue;
            }
            set(slot, new MenuItem(icon.item().build(), e -> {
                clickSound();
                action.run();
            }));
        }
    }
}
