package com.nocrates.editor;

import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.migrate.Importer;
import com.nocrates.migrate.Migrations;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Editor > Migrations: one button per registered importer, greyed out when undetected. */
public final class MigrationsMenu extends Menu {

    public MigrationsMenu(Player viewer) {
        super(viewer, "<dark_gray>Editor <dark_gray>» Migrations", 3);
    }

    @Override
    protected void draw() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        int slot = 10;
        for (Importer importer : Migrations.all()) {
            if (slot > 16) break;
            boolean available = importer.detect();
            var icon = available
                    ? EditorIcons.button(Material.ENDER_CHEST, importer.id(),
                    "Source files found.", "Click to import.")
                    : EditorIcons.button(Material.GRAY_DYE, importer.id(),
                    "No source files found on", "this server.");
            set(slot++, new MenuItem(icon, e -> {
                clickSound();
                if (!available) return;
                viewer.closeInventory();
                Migrations.run(viewer, importer.id());
            }));
        }
        set(22, new MenuItem(EditorIcons.back(), e -> new EditorHub(viewer).open()));
    }
}
