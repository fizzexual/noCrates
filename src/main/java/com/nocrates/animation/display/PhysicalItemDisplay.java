package com.nocrates.animation.display;

import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.RewardDisplayAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import com.nocrates.reward.Reward;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * The rewards physically pop out of the crate as (unpickupable) item entities, then
 * vanish — the real grant happens separately, so nothing can be duped.
 */
public final class PhysicalItemDisplay implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "PHYSICAL_ITEM";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().displayDurationTicks();
        Location anchor = ctx.anchor();
        List<Item> spawned = new ArrayList<>();
        List<Reward> rewards = ctx.outcome();
        for (int i = 0; i < rewards.size() && i < 5; i++) {
            Item item = anchor.getWorld().dropItem(anchor, ctx.displayItem(rewards.get(i)));
            item.setPickupDelay(32767);
            item.setPersistent(false);
            item.setCanMobPickup(false);
            item.setVelocity(new Vector((Math.random() - 0.5) * 0.25, 0.35, (Math.random() - 0.5) * 0.25));
            spawned.add(item);
        }
        spawned.forEach(item -> ctx.onPhaseCleanup(item::remove));
        Compat.playAt(anchor, "ENTITY_ITEM_PICKUP", 0.8f, 0.8f);
        Ticker.run(ctx, duration, 4, tick -> {
            for (Item item : spawned) {
                if (item.isValid()) {
                    Compat.spawn(Compat.particle("END_ROD"), item.getLocation(), 1, 0.05, 0.05, 0.05, 0);
                }
            }
        });
    }
}
