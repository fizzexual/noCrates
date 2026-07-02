package com.nocrates.modules.crateclaim;

import com.nocrates.api.NoCratesApi;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemBuilder;
import com.nocrates.item.ItemSpec;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.Reward;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Paged storage of unclaimed rewards; click to claim, hopper to claim everything. */
public final class ClaimMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final NoCratesApi api;
    private int page;

    public ClaimMenu(Player viewer, NoCratesApi api) {
        super(viewer, api.lang().rawString("claim-menu-title"), 6);
        this.api = api;
    }

    @Override
    protected void draw() {
        List<String> claims = api.players().of(viewer).claims();
        int pages = Math.max(1, (claims.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= claims.size()) break;
            String row = claims.get(index);
            set(i, new MenuItem(iconOf(row), e -> {
                clickSound();
                if (claim(row)) refresh();
            }));
        }
        if (page > 0) {
            set(45, new MenuItem(ItemBuilder.of(Material.ARROW).name("<yellow>Previous").build(), e -> {
                page--;
                refresh();
            }));
        }
        if (page < pages - 1) {
            set(53, new MenuItem(ItemBuilder.of(Material.ARROW).name("<yellow>Next").build(), e -> {
                page++;
                refresh();
            }));
        }
        set(49, new MenuItem(ItemBuilder.of(Material.HOPPER)
                .name(api.lang().rawString("claim-all-button")).build(), e -> {
            clickSound();
            claimAll();
            refresh();
        }));
    }

    private ItemStack iconOf(String row) {
        Reward reward = rewardOf(row);
        if (reward == null) {
            return ItemBuilder.of(Material.BARRIER).name("<red>Unknown reward (crate removed)").build();
        }
        return reward.displayItem().build();
    }

    private Reward rewardOf(String row) {
        String[] parts = row.split(";", 3);
        if (parts.length < 2) return null;
        Crate crate = api.crates().get(parts[0]);
        return crate == null ? null : crate.reward(parts[1]);
    }

    /** Claims one stored reward if the items fit; keeps it stored otherwise. */
    private boolean claim(String row) {
        Reward reward = rewardOf(row);
        var data = api.players().of(viewer);
        if (reward == null) {
            data.removeClaim(row); // crate/reward no longer exists — clear the dead entry
            return true;
        }
        List<ItemSpec> items = reward.winItems();
        if (emptySlots() < Math.max(1, items.size())) {
            api.lang().send(viewer, "open-inventory-full");
            return false;
        }
        if (!data.removeClaim(row)) return false;
        for (ItemSpec spec : items) {
            viewer.getInventory().addItem(spec.build()).values()
                    .forEach(left -> viewer.getWorld().dropItemNaturally(viewer.getLocation(), left));
        }
        api.lang().send(viewer, "claim-claimed",
                Placeholder.parsed("reward", reward.displayName()));
        return true;
    }

    private void claimAll() {
        for (String row : api.players().of(viewer).claims()) {
            if (!claim(row)) break;
        }
    }

    private int emptySlots() {
        int empty = 0;
        for (ItemStack item : viewer.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) empty++;
        }
        return empty;
    }
}
