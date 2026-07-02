package com.nocrates.open;

import com.nocrates.core.Services;
import com.nocrates.key.Key;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuConfig;
import com.nocrates.menu.MenuItem;
import com.nocrates.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** /crates virtualkeys — every key with the viewer's virtual balance. */
public final class VirtualKeysMenu extends Menu {

    private final MenuConfig.Spec spec;

    public VirtualKeysMenu(Player viewer) {
        super(viewer, Services.get().menus().get("virtualkeys").title(),
                Services.get().menus().get("virtualkeys").rows());
        this.spec = Services.get().menus().get("virtualkeys");
    }

    @Override
    protected void draw() {
        var services = Services.get();
        List<Integer> slots = contentSlots();
        int i = 0;
        for (Key key : services.keys().all()) {
            if (i >= slots.size()) break;
            int balance = services.keyService().balance(viewer, key.id());
            ItemStack icon = key.item().build();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null
                        ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Text.mm(services.lang().rawString("virtualkeys-lore-balance")
                        .replace("<amount>", String.valueOf(balance))));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            set(slots.get(i++), new MenuItem(icon));
        }
        MenuConfig.Icon close = spec.icon("close");
        if (close != null) {
            for (int slot : close.slots()) {
                set(slot, new MenuItem(close.item().build(), e -> viewer.closeInventory()));
            }
        }
    }

    private List<Integer> contentSlots() {
        if (!spec.contentSlots().isEmpty()) return spec.contentSlots();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < (spec.rows() - 1) * 9; i++) slots.add(i);
        return slots;
    }
}
