package com.nocrates.key;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KeyLinkOrderTest {

    @Test
    void ordersByPriorityThenId() {
        var links = List.of(
                new KeyLink("b", 1, 2),
                new KeyLink("a", 1, 1),
                new KeyLink("c", 1, 1));
        var ordered = KeyService.ordered(links);
        assertEquals("a", ordered.get(0).keyId());
        assertEquals("c", ordered.get(1).keyId());
        assertEquals("b", ordered.get(2).keyId());
    }

    @Test
    void planTakesVirtualBeforePhysical() {
        var plan = KeyService.plan(
                Map.of("vote", new int[]{2, 5}),
                List.of(new KeyLink("vote", 4, 0)));
        assertArrayEquals(new int[]{2, 2}, plan.get("vote"));
    }

    @Test
    void planIsAllOrNothing() {
        assertNull(KeyService.plan(
                Map.of("vote", new int[]{1, 0}, "rare", new int[]{0, 0}),
                List.of(new KeyLink("vote", 1, 0), new KeyLink("rare", 1, 1))));
    }

    @Test
    void planHandlesRepeatedKeyAcrossLinks() {
        var plan = KeyService.plan(
                Map.of("vote", new int[]{1, 2}),
                List.of(new KeyLink("vote", 1, 0), new KeyLink("vote", 2, 1)));
        assertArrayEquals(new int[]{1, 2}, plan.get("vote"));
        assertNull(KeyService.plan(
                Map.of("vote", new int[]{1, 1}),
                List.of(new KeyLink("vote", 1, 0), new KeyLink("vote", 2, 1))));
    }
}
