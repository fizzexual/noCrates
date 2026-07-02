package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemBuilder;
import com.nocrates.item.ItemSpec;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.Reward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Rewards of one crate: click = edit, shift-click = delete, hand item = new reward. */
public final class RewardListEditor extends Menu {

    private final Crate crate;
    private int page;

    public RewardListEditor(Player viewer, Crate crate) {
        super(viewer, "<dark_gray>Rewards <dark_gray>» <white>" + crate.id(), 6);
        this.crate = crate;
    }

    @Override
    protected void draw() {
        var services = Services.get();
        List<Reward> rewards = crate.rewardList();
        int perPage = 45;
        int pages = Math.max(1, (rewards.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < perPage; i++) {
            int index = page * perPage + i;
            if (index >= rewards.size()) break;
            Reward reward = rewards.get(index);
            ItemStack icon = reward.displayItem().build();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(com.nocrates.text.Text.mm("<gray>id: <white>" + reward.id()));
                lore.add(com.nocrates.text.Text.mm("<gray>weight: <white>" + reward.percentage()
                        + " <gray>(" + String.format(java.util.Locale.ROOT, "%.2f", crate.normalizedChance(reward)) + "%)"));
                lore.add(com.nocrates.text.Text.mm("<yellow>Click <gray>to edit"));
                lore.add(com.nocrates.text.Text.mm("<red>Shift-click <gray>to delete"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            set(i, new MenuItem(icon, e -> {
                clickSound();
                if (e.isShiftClick()) {
                    crate.removeReward(reward.id());
                    services.crates().save(crate);
                    refresh();
                } else {
                    new RewardEditor(viewer, crate, reward).open();
                }
            }));
        }
        set(45, new MenuItem(EditorIcons.back(), e -> new CrateEditor(viewer, crate).open()));
        set(49, new MenuItem(EditorIcons.button(Material.NETHER_STAR, "Add reward from hand",
                "Hold an item and click: it becomes",
                "the display AND the won item."), e -> {
            clickSound();
            ItemStack hand = viewer.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                services.lang().send(viewer, "editor-hold-item");
                return;
            }
            String id = nextId();
            Reward reward = new Reward(id);
            reward.displayItem(ItemSpec.fromItem(hand));
            reward.winItems().add(ItemSpec.fromItem(hand));
            crate.addReward(reward);
            services.crates().save(crate);
            new RewardEditor(viewer, crate, reward).open();
        }));
        if (page > 0) {
            set(48, new MenuItem(ItemBuilder.of(Material.ARROW).name("<yellow>Previous").build(), e -> {
                page--;
                refresh();
            }));
        }
        if (page < pages - 1) {
            set(50, new MenuItem(ItemBuilder.of(Material.ARROW).name("<yellow>Next").build(), e -> {
                page++;
                refresh();
            }));
        }
    }

    private String nextId() {
        int n = crate.rewards().size() + 1;
        while (crate.reward("reward-" + n) != null) n++;
        return "reward-" + n;
    }
}
