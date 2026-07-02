package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemBuilder;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All crates: click to edit, right-click to enable/disable, plus create/clone. */
public final class CrateListEditor extends Menu {

    private int page;

    public CrateListEditor(Player viewer) {
        super(viewer, "<dark_gray>Editor <dark_gray>» Crates", 6);
    }

    @Override
    protected void draw() {
        var services = Services.get();
        List<Crate> crates = new ArrayList<>(services.crates().all());
        int perPage = 45;
        int pages = Math.max(1, (crates.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < perPage; i++) {
            int index = page * perPage + i;
            if (index >= crates.size()) break;
            Crate crate = crates.get(index);
            var icon = ItemBuilder.of(com.nocrates.compat.Compat.material(crate.blockMaterial(), Material.CHEST))
                    .name(crate.displayName())
                    .lore(List.of(
                            "<gray>id: <white>" + crate.id(),
                            "<gray>rewards: <white>" + crate.rewards().size(),
                            crate.enabled() ? "<green>enabled" : "<red>disabled",
                            "",
                            "<yellow>Click <gray>to edit",
                            "<yellow>Right-click <gray>to " + (crate.enabled() ? "disable" : "enable")))
                    .build();
            set(i, new MenuItem(icon, e -> {
                clickSound();
                if (e.isRightClick()) {
                    crate.enabled(!crate.enabled());
                    services.crates().save(crate);
                    refresh();
                } else {
                    new CrateEditor(viewer, crate).open();
                }
            }));
        }
        set(45, new MenuItem(EditorIcons.back(), e -> new EditorHub(viewer).open()));
        set(49, new MenuItem(EditorIcons.button(Material.NETHER_STAR, "Create a crate",
                "Type the new crate id in chat."), e -> {
            clickSound();
            ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
                String id = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
                if (id.isEmpty() || services.crates().exists(id)) {
                    services.lang().send(viewer, "crate-already-exists", Placeholder.unparsed("crate", id));
                } else {
                    Crate crate = services.crates().create(id);
                    services.lang().send(viewer, "crate-created", Placeholder.unparsed("crate", id));
                    new CrateEditor(viewer, crate).open();
                    return;
                }
                new CrateListEditor(viewer).open();
            });
        }));
        if (page > 0) {
            set(48, new MenuItem(EditorIcons.button(Material.ARROW, "Previous page"), e -> {
                page--;
                refresh();
            }));
        }
        if (page < pages - 1) {
            set(50, new MenuItem(EditorIcons.button(Material.ARROW, "Next page"), e -> {
                page++;
                refresh();
            }));
        }
    }
}
