package com.nocrates.reward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PityTest {

    @Test
    void forcesOnEveryNthOpen() {
        Pity pity = new Pity(true, 25, "legendary");
        assertTrue(pity.shouldForce(24), "25th open (prior=24) should force");
        assertTrue(pity.shouldForce(49), "50th open (prior=49) should force");
        assertFalse(pity.shouldForce(0), "1st open should not force");
        assertFalse(pity.shouldForce(23), "24th open should not force");
        assertFalse(pity.shouldForce(25), "26th open should not force");
    }

    @Test
    void disabledNeverForces() {
        Pity pity = Pity.disabled();
        for (int i = 0; i < 100; i++) {
            assertFalse(pity.shouldForce(i));
        }
    }

    @Test
    void zeroEveryNeverForces() {
        Pity pity = new Pity(true, 0, "legendary");
        assertFalse(pity.shouldForce(0));
        assertFalse(pity.shouldForce(5));
    }
}
