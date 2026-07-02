package com.nocrates.reward;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollEngineTest {

    private static Reward reward(String id, double weight) {
        Reward r = new Reward(id);
        r.percentage(weight);
        return r;
    }

    @Test
    void distributionMatchesWeights() {
        RollEngine engine = new RollEngine(new Random(42));
        Reward a = reward("a", 50), b = reward("b", 2);
        int hitsA = 0, n = 100_000;
        for (int i = 0; i < n; i++) {
            if (engine.roll(List.of(a, b)).id().equals("a")) hitsA++;
        }
        double share = hitsA / (double) n;
        assertTrue(Math.abs(share - 50.0 / 52.0) < 0.02, "share was " + share);
    }

    @Test
    void zeroWeightNeverRolls() {
        RollEngine engine = new RollEngine(new Random(7));
        Reward a = reward("a", 0), b = reward("b", 1);
        for (int i = 0; i < 10_000; i++) {
            assertEquals("b", engine.roll(List.of(a, b)).id());
        }
    }

    @Test
    void emptyOrAllZeroPoolRollsNull() {
        RollEngine engine = new RollEngine(new Random(1));
        assertNull(engine.roll(List.of()));
        assertNull(engine.roll(List.of(reward("a", 0))));
    }

    @Test
    void rollNAvoidsDuplicatesUntilPoolExhausts() {
        RollEngine engine = new RollEngine(new Random(3));
        List<Reward> pool = List.of(reward("a", 1), reward("b", 1), reward("c", 1));
        List<Reward> five = engine.rollN(pool, 5, Reward::percentage);
        assertEquals(5, five.size());
        // first three draws must be distinct
        assertEquals(3, five.subList(0, 3).stream().map(Reward::id).distinct().count());
    }
}
