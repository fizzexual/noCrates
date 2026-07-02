package com.nocrates.modules.animationsplus;

import com.nocrates.animation.Displays;
import com.nocrates.animation.OpeningContext;
import com.nocrates.animation.PostOpenAnimation;
import com.nocrates.animation.Ticker;
import com.nocrates.compat.Compat;
import com.nocrates.reward.Reward;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Orbit Roulette (add-on parity): reward items rise and orbit the crate, then converge. */
public final class OrbitRoulettePost implements PostOpenAnimation {

    @Override
    public String id() {
        return "ORBIT_ROULETTE";
    }

    @Override
    public void play(OpeningContext ctx) {
        long duration = ctx.crate().animation().postDelayTicks();
        List<Reward> pool = ctx.crate().rewardList();
        if (pool.isEmpty()) {
            ctx.phaseDone();
            return;
        }
        Random random = new Random();
        Location anchor = ctx.anchor();
        int count = Math.min(6, Math.max(3, pool.size()));
        List<ItemDisplay> orbiters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemDisplay item = Displays.item(anchor.clone(),
                    ctx.displayItem(pool.get(random.nextInt(pool.size()))), 0.5f);
            orbiters.add(item);
            ctx.onPhaseCleanup(item::remove);
        }
        Compat.playAt(anchor, "BLOCK_BEACON_AMBIENT", 0.7f, 1.5f);
        Ticker.run(ctx, duration, 2, tick -> {
            double progress = tick / (double) duration;
            double radius = progress < 0.7 ? 1.6 : 1.6 * (1.0 - (progress - 0.7) / 0.3);
            double y = 0.4 + Math.min(1.0, progress * 2) * 0.8;
            for (int i = 0; i < orbiters.size(); i++) {
                ItemDisplay item = orbiters.get(i);
                if (!item.isValid()) continue;
                double angle = tick * 0.22 + i * 2 * Math.PI / orbiters.size();
                item.teleport(anchor.clone().add(radius * Math.cos(angle), y, radius * Math.sin(angle)));
            }
            if (tick + 2 >= duration) {
                Compat.spawn(Compat.particle("FLASH"), anchor.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
            }
        });
    }
}
