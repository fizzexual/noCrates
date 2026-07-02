package com.nocrates.modules.animationsplus;

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
 * Physical CS:GO (add-on parity): a vertical reel of item displays cycling above the
 * crate with ease-out timing, settling on the winning reward.
 */
public final class PhysicalCsgoPost implements PostOpenAnimation {

    @Override
    public String id() {
        return "PHYSICAL_CSGO";
    }

    @Override
    public void play(OpeningContext ctx) {
        List<Reward> pool = ctx.crate().rewardList();
        if (pool.isEmpty()) {
            ctx.phaseDone();
            return;
        }
        Random random = new Random();
        Location base = ctx.anchor().clone().add(0, 0.4, 0);
        ItemDisplay[] reel = new ItemDisplay[3];
        for (int i = 0; i < 3; i++) {
            reel[i] = Displays.item(base.clone().add(0, i * 0.55, 0),
                    ctx.displayItem(pool.get(random.nextInt(pool.size()))), i == 1 ? 0.7f : 0.45f);
            ctx.onPhaseCleanup(reel[i]::remove);
        }
        int totalShifts = 16 + random.nextInt(5);
        shift(ctx, reel, pool, random, 0, totalShifts);
    }

    private void shift(OpeningContext ctx, ItemDisplay[] reel, List<Reward> pool,
                       Random random, int index, int total) {
        double progress = index / (double) total;
        long delay = Math.max(1, Math.round(1 + Math.pow(progress, 2) * 9));
        Scheduling.later(ctx.plugin(), ctx.anchor(), delay, () -> {
            boolean last = index >= total;
            Reward next = last ? ctx.outcome().get(0) : pool.get(random.nextInt(pool.size()));
            // roll items downward through the reel
            for (int i = 0; i < reel.length - 1; i++) {
                if (reel[i].isValid() && reel[i + 1].isValid()) {
                    reel[i].setItemStack(reel[i + 1].getItemStack());
                }
            }
            if (reel[2].isValid()) reel[2].setItemStack(ctx.displayItem(next));
            Compat.playAt(ctx.anchor(), "UI_BUTTON_CLICK", 0.4f, 0.8f + (float) progress);
            if (last) {
                // center shows the winner
                if (reel[1].isValid()) reel[1].setItemStack(ctx.displayItem(ctx.outcome().get(0)));
                Compat.playAt(ctx.anchor(), "ENTITY_PLAYER_LEVELUP", 0.8f, 1.4f);
                Scheduling.later(ctx.plugin(), ctx.anchor(), 15, ctx::phaseDone);
            } else {
                shift(ctx, reel, pool, random, index + 1, total);
            }
        });
    }
}
