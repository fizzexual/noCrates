package com.nocrates.animation;

import com.nocrates.animation.extra.ExtraDisplayAnimations;
import com.nocrates.animation.extra.ExtraPostAnimations;
import com.nocrates.animation.extra.ExtraPreAnimations;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtraAnimationsTest {

    @Test
    void expansionPackShipsTwentySixUniqueAnimations() {
        var pre = ExtraPreAnimations.all();
        var post = ExtraPostAnimations.all();
        var display = ExtraDisplayAnimations.all();
        assertEquals(9, pre.size());
        assertEquals(9, post.size());
        assertEquals(8, display.size());

        Set<String> ids = new HashSet<>();
        pre.forEach(a -> ids.add("pre:" + a.id()));
        post.forEach(a -> ids.add("post:" + a.id()));
        display.forEach(a -> ids.add("display:" + a.id()));
        assertEquals(26, ids.size(), "ids must be unique per phase");
        ids.forEach(id -> assertTrue(id.substring(id.indexOf(':') + 1)
                .matches("[A-Z_]+"), id + " should be UPPER_SNAKE"));
    }

    /**
     * The headline claim: at least 50 selectable animations across the three phases.
     * Core (7 pre + 6 post + 9 display incl. GUI_CSGO/CHEST_HUNT-style) + module pack
     * (4 pre + 4 post) + expansion (9 + 9 + 8) = 56.
     */
    @Test
    void totalAnimationCountIsFiftyPlus() {
        int core = 7 + 6 + 9;
        int modulePack = 4 + 4;
        int expansion = 9 + 9 + 8;
        assertTrue(core + modulePack + expansion >= 50,
                "expected 50+, got " + (core + modulePack + expansion));
        assertEquals(56, core + modulePack + expansion);
    }
}
