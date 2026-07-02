package com.nocrates.animation;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapeRendererTest {

    private static final Set<String> EXPECTED = Set.of(
            "DEFAULT", "CIRCLE", "SPIRAL", "CONICAL_SPIRAL", "STAR", "NINJA_STAR",
            "SQUARE", "DIAMOND", "ASTROID", "DELTOID", "FLOWER", "QUATREFOIL", "PULSE",
            "HELIX", "HEART");

    @Test
    void allBuiltinShapesExist() {
        Set<String> ids = ShapeRenderer.builtins().stream()
                .map(ShapeRenderer.Shape::id)
                .collect(Collectors.toSet());
        assertEquals(EXPECTED, ids);
    }

    @Test
    void everyShapeStaysWithinRadiusAndProducesPoints() {
        double radius = 1.5;
        for (ShapeRenderer.Shape shape : ShapeRenderer.builtins()) {
            for (int tick = 0; tick < 200; tick += 7) {
                List<Vector> points = shape.points(radius, tick);
                assertFalse(points.isEmpty(), shape.id() + " produced no points at tick " + tick);
                for (Vector p : points) {
                    double horizontal = Math.sqrt(p.getX() * p.getX() + p.getZ() * p.getZ());
                    assertTrue(horizontal <= radius + 0.001,
                            shape.id() + " point " + p + " outside radius at tick " + tick);
                }
            }
        }
    }

    @Test
    void pulseExpandsAndWraps() {
        ShapeRenderer.Shape pulse = ShapeRenderer.builtins().stream()
                .filter(s -> s.id().equals("PULSE")).findFirst().orElseThrow();
        double early = pulse.points(2.0, 3).get(0).length();
        double late = pulse.points(2.0, 27).get(0).length();
        double wrapped = pulse.points(2.0, 33).get(0).length();
        assertTrue(late > early, "pulse should expand");
        assertTrue(wrapped < late, "pulse should wrap around");
    }

    @Test
    void circlePointsSitOnTheRadius() {
        ShapeRenderer.Shape circle = ShapeRenderer.builtins().stream()
                .filter(s -> s.id().equals("CIRCLE")).findFirst().orElseThrow();
        for (Vector p : circle.points(1.25, 42)) {
            assertEquals(1.25, p.length(), 0.001);
        }
    }
}
