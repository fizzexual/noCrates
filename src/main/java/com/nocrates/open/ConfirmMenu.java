package com.nocrates.open;

import com.nocrates.core.Services;
import com.nocrates.item.ItemSpec;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuConfig;
import com.nocrates.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Yes/no confirmation, used for paid crate opens. Styled by menus/confirmation.yml. */
public final class ConfirmMenu extends Menu {

    private final Runnable onConfirm;
    private final String cost;
    private final MenuConfig.Spec spec;
    private boolean decided;

    public ConfirmMenu(Player viewer, String cost, Runnable onConfirm) {
        super(viewer, Services.get().menus().get("confirmation").title().replace("<cost>", cost),
                Services.get().menus().get("confirmation").rows());
        this.cost = cost;
        this.onConfirm = onConfirm;
        this.spec = Services.get().menus().get("confirmation");
    }

    @Override
    protected void draw() {
        icon("confirm", () -> {
            decided = true;
            viewer.closeInventory();
            onConfirm.run();
        });
        icon("cancel", () -> {
            decided = true;
            viewer.closeInventory();
        });
        icon("info", null);
    }

    private void icon(String key, Runnable action) {
        MenuConfig.Icon icon = spec.icon(key);
        if (icon == null) return;
        ItemSpec item = icon.item().copy();
        if (item.name() != null) item.name(item.name().replace("<cost>", cost));
        item.lore(item.lore().stream().map(l -> l.replace("<cost>", cost)).toList());
        for (int slot : icon.slots()) {
            set(slot, new MenuItem(item.build(), e -> {
                clickSound();
                if (action != null) action.run();
            }));
        }
    }
}
