package com.nocrates.reward;

import java.util.List;
import java.util.Random;

/**
 * PhoenixCrates-style "Guaranteed Win" (milestones). SEQUENTIAL walks a one-time list of
 * thresholds; REPETITIVE cycles the list forever, firing every time the player's total
 * opens hit a milestone's openings multiple. A milestone may additionally carry a chance
 * (0-100): with openings > 0 and chance > 0 both must hold; with openings == 0 the
 * milestone becomes purely chance-based and is evaluated on every open.
 */
public final class GuaranteedWin {

    public enum Mode {
        SEQUENTIAL, REPETITIVE
    }

    /** openings = the Nth open this fires on (0 = chance-only); chance 0..100 (0 = openings-only). */
    public record Milestone(int openings, String rewardId, double chance) {
    }

    public record Result(String rewardId, int nextIndex) {
    }

    private GuaranteedWin() {
    }

    /**
     * Evaluates whether the open that is about to complete (the player's
     * {@code totalOpens}-th open, 1-based) triggers a guaranteed reward.
     *
     * @param nextIndex SEQUENTIAL progress pointer (index of the next unclaimed milestone)
     * @return the reward id to force plus the updated pointer, or null when nothing fires
     */
    public static Result check(Mode mode, List<Milestone> milestones, int totalOpens, int nextIndex, Random rng) {
        if (milestones == null || milestones.isEmpty()) return null;
        if (mode == Mode.SEQUENTIAL) {
            if (nextIndex >= milestones.size()) return null;
            Milestone next = milestones.get(nextIndex);
            if (fires(next, totalOpens, rng, false)) {
                return new Result(next.rewardId(), nextIndex + 1);
            }
            return null;
        }
        // REPETITIVE: any milestone whose openings divide the current total fires.
        for (Milestone m : milestones) {
            if (fires(m, totalOpens, rng, true)) {
                return new Result(m.rewardId(), nextIndex);
            }
        }
        return null;
    }

    private static boolean fires(Milestone m, int totalOpens, Random rng, boolean repetitive) {
        boolean openingsOk;
        if (m.openings() <= 0) {
            openingsOk = true; // chance-only milestone
        } else if (repetitive) {
            openingsOk = totalOpens > 0 && totalOpens % m.openings() == 0;
        } else {
            openingsOk = totalOpens >= m.openings();
        }
        if (!openingsOk) return false;
        if (m.chance() <= 0) return m.openings() > 0; // openings-only (pure chance-less)
        return rng.nextDouble() * 100.0 < m.chance();
    }
}
