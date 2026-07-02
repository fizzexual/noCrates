package com.nocrates.reward;

import com.nocrates.reward.GuaranteedWin.Milestone;
import com.nocrates.reward.GuaranteedWin.Mode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuaranteedWinTest {

    private final Random rng = new Random(1);

    @Test
    void sequentialFiresOnceAndAdvances() {
        List<Milestone> ms = List.of(
                new Milestone(5, "bronze", 0),
                new Milestone(10, "silver", 0));
        assertNull(GuaranteedWin.check(Mode.SEQUENTIAL, ms, 4, 0, rng));
        var first = GuaranteedWin.check(Mode.SEQUENTIAL, ms, 5, 0, rng);
        assertEquals("bronze", first.rewardId());
        assertEquals(1, first.nextIndex());
        // pointer advanced: opening 6..9 fire nothing
        assertNull(GuaranteedWin.check(Mode.SEQUENTIAL, ms, 6, 1, rng));
        var second = GuaranteedWin.check(Mode.SEQUENTIAL, ms, 10, 1, rng);
        assertEquals("silver", second.rewardId());
        assertEquals(2, second.nextIndex());
        // list exhausted
        assertNull(GuaranteedWin.check(Mode.SEQUENTIAL, ms, 100, 2, rng));
    }

    @Test
    void repetitiveFiresEveryMultiple() {
        List<Milestone> ms = List.of(new Milestone(25, "pity", 0));
        assertNull(GuaranteedWin.check(Mode.REPETITIVE, ms, 24, 0, rng));
        assertEquals("pity", GuaranteedWin.check(Mode.REPETITIVE, ms, 25, 0, rng).rewardId());
        assertNull(GuaranteedWin.check(Mode.REPETITIVE, ms, 26, 0, rng));
        assertEquals("pity", GuaranteedWin.check(Mode.REPETITIVE, ms, 50, 0, rng).rewardId());
    }

    @Test
    void chanceZeroPercentNeverFiresOnChanceOnlyMilestone() {
        // openings=0 & chance=0 is a dead milestone
        List<Milestone> ms = List.of(new Milestone(0, "never", 0));
        for (int open = 1; open <= 200; open++) {
            assertNull(GuaranteedWin.check(Mode.REPETITIVE, ms, open, 0, rng));
        }
    }

    @Test
    void chanceOnlyMilestoneFiresStatistically() {
        List<Milestone> ms = List.of(new Milestone(0, "lucky", 50));
        Random seeded = new Random(99);
        int fires = 0;
        for (int open = 1; open <= 10_000; open++) {
            if (GuaranteedWin.check(Mode.REPETITIVE, ms, open, 0, seeded) != null) fires++;
        }
        assertEquals(0.5, fires / 10_000.0, 0.05);
    }
}
