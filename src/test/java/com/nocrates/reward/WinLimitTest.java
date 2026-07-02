package com.nocrates.reward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WinLimitTest {

    @Test
    void unlimitedAlwaysAllows() {
        assertTrue(WinLimit.none().allows(999_999, 0, 1000));
    }

    @Test
    void maxBlocksAtLimit() {
        WinLimit limit = new WinLimit(3, 0);
        assertTrue(limit.allows(2, 0, 1000));
        assertFalse(limit.allows(3, 0, 1000));
    }

    @Test
    void cooldownBlocksUntilExpiry() {
        WinLimit limit = new WinLimit(-1, 60);
        assertFalse(limit.allows(0, 2000, 1999));
        assertTrue(limit.allows(0, 2000, 2000));
    }
}
