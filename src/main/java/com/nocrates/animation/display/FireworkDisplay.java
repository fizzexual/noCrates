package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.compat.Scheduling;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

/** Celebration fireworks around the reward showcase. */
public final class FireworkDisplay extends ShowcaseBase implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "FIREWORK";
    }

    @Override
    public void play(OpeningContext ctx) {
        showcase(ctx, tick -> {
            if (tick % 20 != 0 || tick > ctx.crate().animation().displayDurationTicks() - 10) return;
            launch(ctx.anchor().clone().add((Math.random() - 0.5) * 1.6, 0.2, (Math.random() - 0.5) * 1.6), ctx);
        });
    }

    private void launch(Location at, OpeningContext ctx) {
        // World#spawn(Location, Class) is immune to the 1.20.5 EntityType renames.
        Firework firework = at.getWorld().spawn(at, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.fromRGB(0x7B5CFF), Color.fromRGB(0xFF5CA8), Color.fromRGB(0xFFC145))
                .withFade(Color.WHITE)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        // Detonate just above the crate rather than flying away.
        Scheduling.entityLater(ctx.plugin(), firework, 12, () -> {
            if (firework.isValid()) firework.detonate();
        });
    }
}
