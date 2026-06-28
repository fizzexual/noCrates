package com.nocrates.animation;

import com.nocrates.crate.Crate;
import com.nocrates.gui.Menu;
import com.nocrates.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A 27-slot click-locked container used as the canvas for GUI animations.
 * Clicks are cancelled by {@link com.nocrates.gui.MenuListener}; animations
 * drive the contents directly via {@link #getInventory()}.
 */
public final class SpinnerMenu extends Menu {

    public SpinnerMenu(Crate crate) {
        super(27, Items.mini(crate.displayName()));
    }

    @Override
    protected void build(Player player) {
        ItemStack filler = Items.icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        fill(filler);
    }
}
