package com.nocrates.reward;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollEngineTest {

    @Test
    void distributionMatchesWeights() {
        RollEngine engine = new RollEngine(new Random(42));
        Reward a = Reward.of("a", 50);
        Reward b = Reward.of("b", 2);
        int countA = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            if (engine.roll(List.of(a, b)).id().equals("a")) {
                countA++;
            }
        }
        double share = countA / (double) n;
        assertTrue(Math.abs(share - 50.0 / 52.0) < 0.02,
                "expected ~" + (50.0 / 52.0) + " but was " + share);
    }

    @Test
    void zeroWeightIsNeverRolled() {
        RollEngine engine = new RollEngine(new Random(1));
        Reward live = Reward.of("live", 10);
        Reward dead = Reward.of("dead", 0);
        for (int i = 0; i < 10_000; i++) {
            assertEquals("live", engine.roll(List.of(live, dead)).id());
        }
    }

    @Test
    void singleRewardAlwaysReturned() {
        RollEngine engine = new RollEngine(new Random(7));
        Reward only = Reward.of("only", 1);
        assertEquals("only", engine.roll(List.of(only)).id());
    }

    @Test
    void emptyListReturnsNull() {
        RollEngine engine = new RollEngine(new Random(7));
        assertEquals(null, engine.roll(List.of()));
    }

    @Test
    void rollTierFiltersByRarity() {
        RollEngine engine = new RollEngine(new Random(3));
        Reward common = new Reward("c", "common", 100, null, List.of(), -1, 0);
        Reward legend = new Reward("l", "legendary", 1, null, List.of(), -1, 0);
        Reward forced = engine.rollTier(List.of(common, legend), "legendary");
        assertNotNull(forced);
        assertEquals("l", forced.id());
    }
}
