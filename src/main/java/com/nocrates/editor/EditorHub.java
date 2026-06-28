package com.nocrates.editor;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.gui.Menu;
import com.nocrates.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Editor landing screen: every crate as a clickable icon, plus "create new". */
public final class EditorHub extends Menu {

    private final NoCrates plugin;

    public EditorHub(NoCrates plugin) {
        super(54, Items.mini("<dark_gray>noCrates · Editor"));
        this.plugin = plugin;
    }

    @Override
    protected void build(Player player) {
        int slot = 0;
        for (Crate crate : plugin.services().crates().all()) {
            if (slot >= 45) {
                break;
            }
            set(slot++, Items.icon(Material.CHEST, crate.displayName(),
                    "<gray>Animation: <white>" + crate.animation(),
                    "<gray>Rewards: <white>" + crate.rewards().size(),
                    "<gray>Key: <white>" + crate.key().type().name().toLowerCase(Locale.ROOT),
                    " ",
                    "<yellow>Click to edit"),
                    event -> new CrateEditor(plugin, crate.name()).open(player));
        }
        set(49, Items.icon(Material.NETHER_STAR, "<green><bold>Create New Crate",
                "<gray>Click, then type a name in chat"),
                event -> {
                    player.closeInventory();
                    plugin.services().chatPrompts().await(player, name -> create(player, name));
                });
    }

    private void create(Player player, String name) {
        String id = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (id.isEmpty()) {
            plugin.services().messages().send(player, "editor-bad-name");
            open(player);
            return;
        }
        if (plugin.services().crates().exists(id)) {
            plugin.services().messages().send(player, "editor-exists");
            new CrateEditor(plugin, id).open(player);
            return;
        }
        plugin.services().crates().save(Crate.builder(id).displayName("<white>" + name).build());
        new CrateEditor(plugin, id).open(player);
    }
}
