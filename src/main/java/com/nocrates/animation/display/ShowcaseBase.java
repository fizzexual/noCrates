package com.nocrates.animation.display;

import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.Ticker;
import com.nocrates.reward.Reward;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Shared reward showcase: fans the won items out above the crate with floating name
 * tags, bobs/spins them for the display duration, then cleans up and finishes the
 * phase. Subclasses add their signature particles per tick.
 */
abstract class ShowcaseBase {

    /** Spawns showcase displays and runs the phase; {@code extra} adds per-tick flair. */
    protected void showcase(OpeningContext ctx, IntConsumer extra) {
        long duration = ctx.crate().animation().displayDurationTicks();
        List<Reward> rewards = ctx.outcome();
        List<ItemDisplay> items = new ArrayList<>();
        Location base = ctx.anchor().clone().add(0, 0.9, 0);
        double spread = 0.8;
        for (int i = 0; i < rewards.size() && i < 5; i++) {
            double x = (i - (Math.min(rewards.size(), 5) - 1) / 2.0) * spread;
            Location at = base.clone().add(x, 0, 0);
            ItemDisplay item = Displays.item(at, ctx.displayItem(rewards.get(i)), 0.75f);
            items.add(item);
            String name = rewards.get(i).displayName();
            if (name != null && !name.isEmpty()) {
                var text = Displays.text(at.clone().add(0, 0.55, 0), name);
                ctx.onPhaseCleanup(text::remove);
            }
        }
        items.forEach(item -> ctx.onPhaseCleanup(item::remove));
        final Location[] anchors = items.stream().map(ItemDisplay::getLocation).toArray(Location[]::new);
        Ticker.run(ctx, duration, 2, tick -> {
            for (int i = 0; i < items.size(); i++) {
                ItemDisplay item = items.get(i);
                if (!item.isValid()) continue;
                Displays.spin(item, 10);
                double bob = Math.sin(tick * 0.12 + i) * 0.06;
                item.teleport(anchors[i].clone().add(0, bob, 0));
            }
            extra.accept(tick);
        });
    }
}
