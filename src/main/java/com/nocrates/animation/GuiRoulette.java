package com.nocrates.animation;

import com.nocrates.compat.Compat;
import com.nocrates.compat.Scheduling;
import com.nocrates.core.Services;
import com.nocrates.item.ItemBuilder;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.Reward;
import org.bukkit.Material;

import java.util.List;
import java.util.Random;

/**
 * The GUI CS:GO spinner: a 9-slot reel scrolling under a pointer with ease-out timing,
 * always landing the real outcome under the pointer. Registered as reward-display
 * "GUI_CSGO" — ideal for virtual opens.
 */
public final class GuiRoulette implements RewardDisplayAnimation {

    @Override
    public String id() {
        return "GUI_CSGO";
    }

    @Override
    public void play(OpeningContext ctx) {
        RouletteMenu menu = new RouletteMenu(ctx);
        menu.open();
        menu.spin();
    }

    private static final class RouletteMenu extends Menu {

        private static final int[] REEL_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
        private static final int POINTER_TOP = 4;
        private static final int POINTER_BOTTOM = 22;

        private final OpeningContext ctx;
        private final Random random = new Random();
        private final List<Reward> pool;
        /** Reel content, index 0 = leftmost slot. */
        private final Reward[] reel = new Reward[9];
        private boolean finished;

        private RouletteMenu(OpeningContext ctx) {
            super(ctx.player(), "<dark_gray>" + ctx.crate().displayName(), 3);
            this.ctx = ctx;
            this.pool = ctx.crate().rewardList();
            for (int i = 0; i < reel.length; i++) reel[i] = randomReward();
        }

        private Reward randomReward() {
            return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
        }

        @Override
        protected void draw() {
            var pointer = new MenuItem(ItemBuilder.of(Material.SPECTRAL_ARROW).name("<yellow>▼").build());
            var pane = new MenuItem(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
            for (int slot = 0; slot < 27; slot++) {
                if (slot == POINTER_TOP || slot == POINTER_BOTTOM) {
                    set(slot, pointer);
                } else if (slot < 9 || slot >= 18) {
                    set(slot, pane);
                }
            }
            renderReel();
        }

        private void renderReel() {
            for (int i = 0; i < REEL_SLOTS.length; i++) {
                Reward reward = reel[i];
                if (reward != null) {
                    set(REEL_SLOTS[i], new MenuItem(ctx.displayItem(reward)));
                }
            }
        }

        /**
         * Ease-out spin: shift steps happen with growing delays; the winning reward is
         * injected so that it sits under the pointer on the final shift.
         */
        private void spin() {
            int totalShifts = 24 + random.nextInt(6);
            scheduleShift(0, totalShifts);
        }

        private void scheduleShift(int shiftIndex, int totalShifts) {
            long delay = delayFor(shiftIndex, totalShifts);
            // player-owned scheduling: inventory mutation must happen on the viewer's
            // region thread on Folia, never on the global scheduler
            Scheduling.entityLater(ctx.plugin(), viewer, delay, () -> {
                if (finished) return;
                if (!viewer.isOnline()) {
                    complete();
                    return;
                }
                // shift left; inject the winner so it lands on the center slot at the end
                System.arraycopy(reel, 1, reel, 0, reel.length - 1);
                int shiftsLeft = totalShifts - shiftIndex - 1;
                reel[8] = shiftsLeft == 4 ? ctx.outcome().get(0) : randomReward();
                renderReel();
                Compat.play(viewer, "UI_BUTTON_CLICK", 0.4f, 0.9f + 1.1f * shiftIndex / totalShifts);
                if (shiftsLeft <= 0) {
                    land();
                } else {
                    scheduleShift(shiftIndex + 1, totalShifts);
                }
            });
        }

        private long delayFor(int shiftIndex, int totalShifts) {
            double progress = shiftIndex / (double) totalShifts;
            return Math.max(1, Math.round(1 + Math.pow(progress, 2.2) * 11));
        }

        private void land() {
            // make absolutely sure the pointer slot shows the outcome
            reel[4] = ctx.outcome().get(0);
            renderReel();
            Compat.play(viewer, "ENTITY_PLAYER_LEVELUP", 0.9f, 1.4f);
            Scheduling.entityLater(ctx.plugin(), viewer, 30, () -> {
                if (!finished && viewer.isOnline()) viewer.closeInventory();
                complete();
            });
        }

        private void complete() {
            if (finished) return;
            finished = true;
            ctx.phaseDone();
        }

        @Override
        protected void onClose() {
            // Closing early never loses the reward: complete the phase immediately.
            complete();
        }
    }
}
