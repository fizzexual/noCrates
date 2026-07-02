package com.nocrates.open;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.item.ItemBuilder;
import com.nocrates.key.KeyLink;
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
 * SELECTIVE rewards mode: the player *chooses* a reward and pays its key cost — a
 * non-gambling opening. Blocked rewards (limits/permissions) show but cannot be taken.
 */
public final class SelectiveMenu extends Menu {

    private final Crate crate;
    private final CratePlacement placement;
    private final MenuConfig.Spec spec;
    private int page;

    public SelectiveMenu(Player viewer, Crate crate, CratePlacement placement) {
        super(viewer, Services.get().menus().get("selective").title().replace("<crate>", crate.displayName()),
                Services.get().menus().get("selective").rows());
        this.crate = crate;
        this.placement = placement;
        this.spec = Services.get().menus().get("selective");
    }

    @Override
    protected void draw() {
        List<Integer> slots = contentSlots();
        // always-rewards are not choices — they come with every pick automatically
        List<Reward> rewards = crate.rewardList().stream().filter(r -> !r.always()).toList();
        int perPage = slots.size();
        int pages = Math.max(1, (rewards.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);

        var services = Services.get();
        var data = services.players().of(viewer);
        for (int i = 0; i < perPage; i++) {
            int index = page * perPage + i;
            if (index >= rewards.size()) break;
            Reward reward = rewards.get(index);
            boolean allowed = services.openService().isAllowed(viewer, data, crate, reward);
            set(slots.get(i), new MenuItem(icon(reward, allowed), e -> {
                clickSound();
                if (allowed) pick(reward);
            }));
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

    private ItemStack icon(Reward reward, boolean allowed) {
        ItemStack icon = reward.displayItem().build();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() && meta.lore() != null
                    ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            var lang = Services.get().lang();
            lore.add(Component.empty());
            lore.add(Text.mm(lang.rawString("selective-lore-cost")
                    .replace("<cost>", String.valueOf(reward.selectiveCost()))));
            lore.add(Text.mm(lang.rawString(allowed ? "selective-lore-pick" : "selective-lore-blocked")));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private void pick(Reward reward) {
        var services = Services.get();
        List<KeyLink> scaled = new ArrayList<>();
        for (KeyLink link : crate.keys()) {
            scaled.add(new KeyLink(link.keyId(), link.amount() * reward.selectiveCost(), link.priority()));
        }
        if (!scaled.isEmpty() && !services.keyService().consume(viewer, scaled)) {
            services.lang().send(viewer, "selective-not-enough-keys",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed(
                            "amount", String.valueOf(reward.selectiveCost())));
            return;
        }
        viewer.closeInventory();
        var data = services.players().of(viewer);
        data.incrOpens(crate.id());
        services.actionLogger().open(viewer.getName(), crate.id());
        // the chosen reward plus every always-reward (guaranteed items)
        List<Reward> outcome = new ArrayList<>(List.of(reward));
        for (Reward extra : crate.rewards().values()) {
            if (extra.always() && !outcome.contains(extra)) outcome.add(extra);
        }
        boolean[] alternative = new boolean[outcome.size()];
        for (int i = 0; i < outcome.size(); i++) {
            alternative[i] = !services.openService().isAllowed(viewer, data, crate, outcome.get(i));
        }
        OpenSession session = new OpenSession(viewer, crate, placement, true, outcome, alternative, 0);
        session.grantAll();
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
