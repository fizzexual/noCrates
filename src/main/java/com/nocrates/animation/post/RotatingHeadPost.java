package com.nocrates.animation.post;

import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/** The opener's own head spins above the crate in a halo of villager sparkles. */
public final class RotatingHeadPost implements PostOpenAnimation {

    @Override
    public String id() {
        return "ROTATING_HEAD";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(ctx.player());
            head.setItemMeta(meta);
        }
        ItemDisplay display = Displays.item(ctx.anchor().clone().add(0, 0.6, 0), head, 0.9f);
        display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        ctx.onPhaseCleanup(display::remove);
        Ticker.run(ctx, duration, 2, tick -> {
            Displays.spin(display, 20);
            Compat.spawn(Compat.particle("HAPPY_VILLAGER"), ctx.anchor().clone().add(0, 0.8, 0), 2, 0.5, 0.4, 0.5, 0);
        });
    }
}
