package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** /crates editor — entry point. */
public final class EditorHub extends Menu {

    public EditorHub(Player viewer) {
        super(viewer, "<dark_gray>noCrates Editor", 3);
    }

    @Override
    protected void draw() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        set(11, new MenuItem(EditorIcons.button(Material.CHEST, "Crates",
                "Create, edit, clone and delete crates."), e -> {
            clickSound();
            new CrateListEditor(viewer).open();
        }));
        set(13, new MenuItem(EditorIcons.button(Material.TRIPWIRE_HOOK, "Keys",
                "Edit key items, virtual flags", "and rarity guarantees."), e -> {
            clickSound();
            new KeyListEditor(viewer).open();
        }));
        set(15, new MenuItem(EditorIcons.button(Material.ENDER_CHEST, "Migrations",
                "Import crates from other plugins."), e -> {
            clickSound();
            new MigrationsMenu(viewer).open();
        }));
        set(22, new MenuItem(EditorIcons.button(Material.COMPARATOR, "Reload",
                "Reload every config file."), e -> {
            clickSound();
            viewer.closeInventory();
            viewer.performCommand("crates reload");
        }));
    }

    public static void register() {
        com.nocrates.command.CratesCommand.registerExtra("editor", "nocrates.editor", (sender, args) -> {
            if (!(sender instanceof Player player)) {
                Services.get().lang().send(sender, "player-only");
                return;
            }
            new EditorHub(player).open();
        }, null);
        com.nocrates.command.CratesCommand.registerExtra("edit", "nocrates.editor", (sender, args) -> {
            if (!(sender instanceof Player player)) {
                Services.get().lang().send(sender, "player-only");
                return;
            }
            var crate = args.length > 0 ? Services.get().crates().get(args[0]) : null;
            if (crate == null) {
                new CrateListEditor(player).open();
            } else {
                new CrateEditor(player, crate).open();
            }
        }, (sender, args) -> args.length == 1
                ? new java.util.ArrayList<>(Services.get().crates().ids()) : java.util.List.of());
    }
}
