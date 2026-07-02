package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.item.ItemSpec;
import com.nocrates.key.Key;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Global key registry editor: item-from-hand, virtual flag, rarity guarantees,
 * create/delete keys.
 */
public final class KeyListEditor extends Menu {

    public KeyListEditor(Player viewer) {
        super(viewer, "<dark_gray>Editor <dark_gray>» Keys", 6);
    }

    @Override
    protected void draw() {
        var services = Services.get();
        int slot = 0;
        for (Key key : services.keys().all()) {
            if (slot >= 45) break;
            var icon = com.nocrates.item.ItemBuilder.of(key.item().build())
                    .lore(List.of(
                            "<gray>id: <white>" + key.id(),
                            "<gray>virtual only: <white>" + key.virtual(),
                            "<gray>guarantees: <white>" + (key.guaranteeRarities().isEmpty()
                                    ? "none" : String.join(", ", key.guaranteeRarities())),
                            "",
                            "<yellow>Click <gray>set item from hand",
                            "<yellow>Right-click <gray>toggle virtual",
                            "<yellow>Drop key (Q) <gray>edit rarity guarantee",
                            "<red>Shift-click <gray>delete key"))
                    .build();
            set(slot++, new MenuItem(icon, e -> {
                clickSound();
                if (e.isShiftClick()) {
                    services.keys().remove(key.id());
                    services.keys().save();
                    refresh();
                    return;
                }
                if (e.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                    ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
                        if (input.equalsIgnoreCase("none")) {
                            key.guaranteeRarities(List.of());
                        } else {
                            key.guaranteeRarities(new ArrayList<>(List.of(
                                    input.toLowerCase(Locale.ROOT).split("[ ,]+"))));
                        }
                        services.keys().save();
                        services.lang().send(viewer, "editor-saved");
                        new KeyListEditor(viewer).open();
                    }, () -> new KeyListEditor(viewer).open());
                    return;
                }
                if (e.isRightClick()) {
                    key.virtual(!key.virtual());
                    services.keys().save();
                    refresh();
                    return;
                }
                var hand = viewer.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) {
                    services.lang().send(viewer, "editor-hold-item");
                    return;
                }
                key.item(ItemSpec.fromItem(hand));
                services.keys().save();
                services.lang().send(viewer, "editor-saved");
                refresh();
            }));
        }
        set(45, new MenuItem(EditorIcons.back(), e -> new EditorHub(viewer).open()));
        set(49, new MenuItem(EditorIcons.button(Material.NETHER_STAR, "Create a key",
                "Type the new key id in chat."), e -> {
            clickSound();
            ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
                String id = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
                if (!id.isEmpty()) {
                    services.keys().getOrCreate(id);
                    services.keys().save();
                    services.lang().send(viewer, "editor-saved");
                }
                new KeyListEditor(viewer).open();
            }, () -> new KeyListEditor(viewer).open());
        }));
    }
}
