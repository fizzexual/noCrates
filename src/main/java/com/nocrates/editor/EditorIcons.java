package com.nocrates.editor;

import com.nocrates.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Consistent editor buttons. */
final class EditorIcons {

    private EditorIcons() {
    }

    static ItemStack button(Material material, String name, String... loreLines) {
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add("<gray>" + line);
        return ItemBuilder.of(material).name("<yellow>" + name).lore(lore).hideAttributes().build();
    }

    static ItemStack toggle(String name, boolean state, String... loreLines) {
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add("<gray>" + line);
        lore.add(state ? "<green>Enabled — click to disable" : "<red>Disabled — click to enable");
        return ItemBuilder.of(state ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<yellow>" + name).lore(lore).build();
    }

    static ItemStack back() {
        return ItemBuilder.of(Material.OAK_DOOR).name("<gray>« Back").build();
    }
}
