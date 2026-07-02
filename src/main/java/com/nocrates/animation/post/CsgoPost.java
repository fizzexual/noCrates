package com.nocrates.animation.post;

import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.compat.Compat;
import com.nocrates.compat.Scheduling;
import com.nocrates.reward.Reward;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

import java.util.List;
import java.util.Random;

/**
 * The classic in-world CS:GO reel: a horizontal line of item displays scrolling past a
 * dust pointer above the crate, decelerating until the winning reward stops dead center.
 */
public final class CsgoPost implements PostOpenAnimation {

    private static final int SLOTS = 7;
    private static final double SPACING = 0.55;

    @Override
    public String id() {
        return "CSGO";
    }

    @Override
    public void play(OpeningContext ctx) {
        List<Reward> pool = ctx.crate().rewardList();
        if (pool.isEmpty()) {
            ctx.phaseDone();
            return;
        }
        Random random = new Random();
        Location center = ctx.anchor().clone().add(0, 1.1, 0);
        ItemDisplay[] reel = new ItemDisplay[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            Location at = slotLocation(center, i);
            float scale = i == SLOTS / 2 ? 0.65f : 0.4f;
            reel[i] = Displays.item(at, ctx.displayItem(pool.get(random.nextInt(pool.size()))), scale);
            ctx.onPhaseCleanup(reel[i]::remove);
        }
        Compat.playAt(center, "BLOCK_BEACON_ACTIVATE", 0.6f, 1.6f);
        int totalShifts = 22 + random.nextInt(6);
        shift(ctx, reel, center, pool, random, 0, totalShifts);
    }

    private Location slotLocation(Location center, int slot) {
        return center.clone().add((slot - SLOTS / 2) * SPACING, 0, 0);
    }

    private void shift(OpeningContext ctx, ItemDisplay[] reel, Location center,
                       List<Reward> pool, Random random, int index, int total) {
        double progress = index / (double) total;
        long delay = Math.max(1, Math.round(1 + Math.pow(progress, 2.1) * 10));
        Scheduling.later(ctx.plugin(), center, delay, () -> {
            boolean last = index >= total;
            // items march left: slot i takes slot i+1's item; a fresh one enters right.
            // The winner is injected so it lands center on the final shift.
            for (int i = 0; i < SLOTS - 1; i++) {
                if (reel[i].isValid() && reel[i + 1].isValid()) {
                    reel[i].setItemStack(reel[i + 1].getItemStack());
                }
            }
            int shiftsLeft = total - index;
            Reward incoming = shiftsLeft == SLOTS / 2 + 1
                    ? ctx.outcome().get(0)
                    : pool.get(random.nextInt(pool.size()));
            if (reel[SLOTS - 1].isValid()) reel[SLOTS - 1].setItemStack(ctx.displayItem(incoming));

            // pointer + track dust
            Compat.spawnDust(center.clone().add(0, 0.55, 0), org.bukkit.Color.YELLOW, 1.2f, 1, 0, 0, 0);
            Compat.spawnDust(center.clone().add(0, -0.45, 0), org.bukkit.Color.YELLOW, 1.2f, 1, 0, 0, 0);
            Compat.playAt(center, "UI_BUTTON_CLICK", 0.45f, 0.8f + (float) progress);

            if (last) {
                if (reel[SLOTS / 2].isValid()) {
                    reel[SLOTS / 2].setItemStack(ctx.displayItem(ctx.outcome().get(0)));
                }
                Compat.spawn(Compat.particle("FIREWORK"), center, 15, 0.4, 0.2, 0.2, 0.05);
                Compat.playAt(center, "ENTITY_PLAYER_LEVELUP", 0.9f, 1.5f);
                Scheduling.later(ctx.plugin(), center, 16, ctx::phaseDone);
            } else {
                shift(ctx, reel, center, pool, random, index + 1, total);
            }
        });
    }
}
