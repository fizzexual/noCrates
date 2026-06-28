package com.nocrates.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataTest {

    @Test
    void giveAndTakeVirtualKeys() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.addKeys("vote", "vote", 3);
        assertEquals(3, data.keys("vote", "vote"));
        assertTrue(data.takeKeys("vote", "vote", 1));
        assertEquals(2, data.keys("vote", "vote"));
    }

    @Test
    void takingMoreThanHeldFailsAndIsClamped() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.addKeys("vote", "vote", 2);
        assertFalse(data.takeKeys("vote", "vote", 5));
        assertEquals(2, data.keys("vote", "vote"), "balance must be unchanged on failure");
    }

    @Test
    void opensAndWinsCount() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.incrOpens("vote");
        data.incrOpens("vote");
        assertEquals(2, data.opens("vote"));
        data.incrWin("vote", "diamonds");
        assertEquals(1, data.winCount("vote", "diamonds"));
        assertEquals(0, data.winCount("vote", "missing"));
    }

    @Test
    void keysAreCaseInsensitive() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.addKeys("Vote", "Vote", 1);
        assertEquals(1, data.keys("vote", "vote"));
    }
}
