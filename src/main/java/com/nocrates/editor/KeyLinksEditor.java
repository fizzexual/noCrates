package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.key.Key;
import com.nocrates.key.KeyLink;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The crate side of key linking: which keys this crate consumes. Click cycles the
 * amount (1-9), right-click cycles priority (0-5), shift-click removes the link.
 * The bottom row lists unlinked keys to add.
 */
public final class KeyLinksEditor extends Menu {

    private final Crate crate;

    public KeyLinksEditor(Player viewer, Crate crate) {
        super(viewer, "<dark_gray>Keys <dark_gray>» <white>" + crate.id(), 6);
        this.crate = crate;
    }

    private void save() {
        Services.get().crates().save(crate);
    }

    @Override
    protected void draw() {
        var services = Services.get();
        List<KeyLink> links = new ArrayList<>(crate.keys());
        for (int i = 0; i < links.size() && i < 36; i++) {
            KeyLink link = links.get(i);
            Key key = services.keys().get(link.keyId());
            var base = key != null ? key.item().build()
                    : com.nocrates.item.ItemBuilder.of(Material.BARRIER).name("<red>" + link.keyId()).build();
            var icon = com.nocrates.item.ItemBuilder.of(base)
                    .lore(List.of(
                            "<gray>key: <white>" + link.keyId(),
                            "<gray>amount: <white>" + link.amount(),
                            "<gray>priority: <white>" + link.priority(),
                            "",
                            "<yellow>Click <gray>amount +1 (wraps at 9)",
                            "<yellow>Right-click <gray>priority +1 (wraps at 5)",
                            "<red>Shift-click <gray>remove link"))
                    .amount(link.amount())
                    .build();
            final int index = i;
            set(i, new MenuItem(icon, e -> {
                clickSound();
                List<KeyLink> updated = new ArrayList<>(crate.keys());
                if (e.isShiftClick()) {
                    updated.remove(index);
                } else if (e.isRightClick()) {
                    updated.set(index, new KeyLink(link.keyId(), link.amount(), (link.priority() + 1) % 6));
                } else {
                    updated.set(index, new KeyLink(link.keyId(), link.amount() % 9 + 1, link.priority()));
                }
                crate.keys(updated);
                save();
                refresh();
            }));
        }
        // addable keys
        int slot = 45;
        for (Key key : services.keys().all()) {
            if (slot > 52) break;
            boolean linked = crate.keys().stream().anyMatch(l -> l.keyId().equals(key.id()));
            if (linked) continue;
            var icon = com.nocrates.item.ItemBuilder.of(key.item().build())
                    .lore(List.of("<green>Click to link <white>" + key.id(), "<gray>to this crate."))
                    .build();
            set(slot++, new MenuItem(icon, e -> {
                clickSound();
                List<KeyLink> updated = new ArrayList<>(crate.keys());
                updated.add(new KeyLink(key.id(), 1, updated.size()));
                crate.keys(updated);
                save();
                refresh();
            }));
        }
        set(53, new MenuItem(EditorIcons.back(), e -> new CrateEditor(viewer, crate).open()));
    }
}
