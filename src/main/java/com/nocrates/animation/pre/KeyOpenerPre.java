package com.nocrates.animation.pre;

import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PreOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import com.nocrates.core.Services;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

/** The crate's key floats above, turns, and sinks into the lock. */
public final class KeyOpenerPre implements PreOpenAnimation {

    @Override
    public String id() {
        return "KEY_OPENER";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().preDelayTicks();
        Location start = ctx.anchor().clone().add(0, 1.0, 0);
        ItemDisplay key = Displays.item(start, keyItem(ctx), 0.7f);
        ctx.onPhaseCleanup(key::remove);
        double drop = 1.0 / Math.max(1, duration);
        Ticker.run(ctx, duration, 2, tick -> {
            Displays.spin(key, 24);
            key.teleport(key.getLocation().subtract(0, drop * 2, 0));
            if (tick % 8 == 0) Compat.playAt(ctx.anchor(), "BLOCK_TRIPWIRE_CLICK_ON", 0.6f, 1.4f);
            if (tick + 2 >= duration) {
                Compat.spawn(Compat.particle("WAX_OFF"), ctx.anchor(), 10, 0.3, 0.3, 0.3, 0.4);
                Compat.playAt(ctx.anchor(), "BLOCK_IRON_TRAPDOOR_OPEN", 0.9f, 1.2f);
            }
        });
    }

    private ItemStack keyItem(OpeningContext ctx) {
        if (!ctx.crate().keys().isEmpty()) {
            var key = Services.get().keys().get(ctx.crate().keys().get(0).keyId());
            if (key != null) return key.item().build();
        }
        return new ItemStack(Material.TRIPWIRE_HOOK);
    }
}
