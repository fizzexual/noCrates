package com.nocrates.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EffectSpecTest {

    @Test
    void parsesFullSpec() {
        EffectSpec spec = EffectSpec.parse("SPIRAL;{DUST;#7b5cff;0;0.2;0;1.2;0.05;2}");
        assertEquals("SPIRAL", spec.shape());
        assertEquals("DUST", spec.particle());
        assertEquals("#7b5cff", spec.hexColor());
        assertEquals(0.2, spec.offY());
        assertEquals(1.2, spec.radius());
        assertEquals(0.05, spec.velocity());
        assertEquals(2, spec.amount());
    }

    @Test
    void colorIsOptional() {
        EffectSpec spec = EffectSpec.parse("CIRCLE;{FLAME;;0;0;0;1;0;1}");
        assertNull(spec.hexColor());
        assertEquals("FLAME", spec.particle());
    }

    @Test
    void defaultsWhenShort() {
        EffectSpec spec = EffectSpec.parse("PULSE;{HAPPY_VILLAGER}");
        assertEquals(1.0, spec.radius());
        assertEquals(1, spec.amount());
    }

    @Test
    void serializeRoundTrip() {
        String line = "STAR;{DUST;#ff0000;0;0.5;0;1.5;0;3}";
        assertEquals(line, EffectSpec.parse(line).serialize());
        EffectSpec reparsed = EffectSpec.parse(EffectSpec.parse(line).serialize());
        assertEquals(EffectSpec.parse(line), reparsed);
    }

    @Test
    void garbageThrows() {
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.parse("garbage"));
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.parse("STAR;no-braces"));
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.parse("STAR;{}"));
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.parse("STAR;{DUST;zzz}"));
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.parse("STAR;{DUST;#fff}"));
    }
}
