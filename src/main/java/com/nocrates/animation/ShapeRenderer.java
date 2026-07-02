package com.nocrates.animation;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * The 13 built-in parametric idle shapes: DEFAULT, CIRCLE,
 * SPIRAL, CONICAL_SPIRAL, STAR, NINJA_STAR, SQUARE, DIAMOND, ASTROID, DELTOID, FLOWER,
 * QUATREFOIL, PULSE. Shapes rotate/animate with the tick counter and return offsets
 * from the crate anchor. Pure math — unit tested.
 */
public final class ShapeRenderer {

    private ShapeRenderer() {
    }

    /** Registers every built-in shape on the animation service. */
    public static void registerAll(AnimationService animations) {
        for (Shape shape : builtins()) animations.register(shape);
    }

    public static List<Shape> builtins() {
        List<Shape> shapes = new ArrayList<>();

        // Slowly rotating ring pair.
        shapes.add(new Shape("DEFAULT", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(2);
            double angle = tick * 0.12;
            points.add(polar(radius, angle, 0));
            points.add(polar(radius, angle + Math.PI, 0));
            return points;
        }));

        shapes.add(new Shape("CIRCLE", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(8);
            double base = tick * 0.05;
            for (int i = 0; i < 8; i++) {
                points.add(polar(radius, base + i * Math.PI / 4, 0));
            }
            return points;
        }));

        // Flat rotating spiral arm rising and wrapping.
        shapes.add(new Shape("SPIRAL", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(3);
            for (int arm = 0; arm < 3; arm++) {
                double t = (tick % 60) / 60.0;
                double angle = tick * 0.18 + arm * (2 * Math.PI / 3);
                points.add(polar(radius * (0.3 + 0.7 * t), angle, 0));
            }
            return points;
        }));

        // Spiral climbing a cone.
        shapes.add(new Shape("CONICAL_SPIRAL", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(2);
            double t = (tick % 80) / 80.0;
            double angle = tick * 0.25;
            double r = radius * (1.0 - t);
            points.add(polar(r, angle, t * 1.8));
            points.add(polar(r, angle + Math.PI, t * 1.8));
            return points;
        }));

        shapes.add(new Shape("STAR", polygonStar(5, 0.45)));
        shapes.add(new Shape("NINJA_STAR", polygonStar(4, 0.30)));
        shapes.add(new Shape("SQUARE", polygon(4)));
        shapes.add(new Shape("DIAMOND", polygon(4, Math.PI / 4)));

        // Classic curves: astroid x=r cos^3, y=r sin^3; deltoid; rose curves.
        shapes.add(new Shape("ASTROID", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(6);
            double spin = tick * 0.03;
            for (int i = 0; i < 6; i++) {
                double t = tick * 0.08 + i * Math.PI / 3;
                double x = radius * Math.pow(Math.cos(t), 3);
                double z = radius * Math.pow(Math.sin(t), 3);
                points.add(rotateY(new Vector(x, 0, z), spin));
            }
            return points;
        }));

        shapes.add(new Shape("DELTOID", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(6);
            double spin = tick * 0.03;
            double b = radius / 3.0;
            for (int i = 0; i < 6; i++) {
                double t = tick * 0.08 + i * Math.PI / 3;
                double x = 2 * b * Math.cos(t) + b * Math.cos(2 * t);
                double z = 2 * b * Math.sin(t) - b * Math.sin(2 * t);
                points.add(rotateY(new Vector(x, 0, z), spin));
            }
            return points;
        }));

        shapes.add(new Shape("FLOWER", rose(3)));
        shapes.add(new Shape("QUATREFOIL", rose(2)));

        // Ring expanding outwards and restarting.
        shapes.add(new Shape("PULSE", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(12);
            double t = (tick % 30) / 30.0;
            double r = radius * t;
            for (int i = 0; i < 12; i++) {
                points.add(polar(r, i * Math.PI / 6, 0));
            }
            return points;
        }));

        // Double helix climbing and wrapping around the crate.
        shapes.add(new Shape("HELIX", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(2);
            double t = (tick % 50) / 50.0;
            double angle = tick * 0.22;
            points.add(polar(radius, angle, t * 2.0));
            points.add(polar(radius, angle + Math.PI, t * 2.0));
            return points;
        }));

        // Classic heart curve drawn upright, slowly spinning. Scaled into the radius.
        shapes.add(new Shape("HEART", (radius, tick) -> {
            List<Vector> points = new ArrayList<>(8);
            double spin = tick * 0.04;
            for (int i = 0; i < 8; i++) {
                double t = tick * 0.09 + i * Math.PI / 4;
                double x = Math.pow(Math.sin(t), 3);                                   // -1..1
                double y = (13 * Math.cos(t) - 5 * Math.cos(2 * t)
                        - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16.0;              // ~-1..1
                Vector flat = new Vector(x * radius * 0.9, y * radius * 0.5 + radius * 0.4, 0);
                points.add(rotateY(flat, spin));
            }
            return points;
        }));

        return shapes;
    }

    private static BiFunction<Double, Integer, List<Vector>> polygon(int sides) {
        return polygon(sides, 0);
    }

    private static BiFunction<Double, Integer, List<Vector>> polygon(int sides, double phase) {
        return (radius, tick) -> {
            List<Vector> points = new ArrayList<>();
            double spin = tick * 0.04 + phase;
            // walk the perimeter: 3 points per edge
            for (int edge = 0; edge < sides; edge++) {
                Vector a = polar(radius, spin + edge * 2 * Math.PI / sides, 0);
                Vector b = polar(radius, spin + (edge + 1) * 2 * Math.PI / sides, 0);
                for (int step = 0; step < 3; step++) {
                    double f = step / 3.0;
                    points.add(a.clone().multiply(1 - f).add(b.clone().multiply(f)));
                }
            }
            return points;
        };
    }

    private static BiFunction<Double, Integer, List<Vector>> polygonStar(int spikes, double innerRatio) {
        return (radius, tick) -> {
            List<Vector> points = new ArrayList<>();
            double spin = tick * 0.04;
            int vertices = spikes * 2;
            for (int v = 0; v < vertices; v++) {
                double rA = v % 2 == 0 ? radius : radius * innerRatio;
                double rB = (v + 1) % 2 == 0 ? radius : radius * innerRatio;
                Vector a = polar(rA, spin + v * Math.PI / spikes, 0);
                Vector b = polar(rB, spin + (v + 1) * Math.PI / spikes, 0);
                for (int step = 0; step < 2; step++) {
                    double f = step / 2.0;
                    points.add(a.clone().multiply(1 - f).add(b.clone().multiply(f)));
                }
            }
            return points;
        };
    }

    /** Rose curve r = R * cos(k * theta) — k=3 gives 3 petals, k=2 gives 4 (quatrefoil). */
    private static BiFunction<Double, Integer, List<Vector>> rose(int k) {
        return (radius, tick) -> {
            List<Vector> points = new ArrayList<>(6);
            double spin = tick * 0.02;
            for (int i = 0; i < 6; i++) {
                double theta = tick * 0.07 + i * Math.PI / 3;
                double r = Math.abs(radius * Math.cos(k * theta));
                points.add(rotateY(polar(r, theta, 0), spin));
            }
            return points;
        };
    }

    private static Vector polar(double radius, double angle, double y) {
        return new Vector(radius * Math.cos(angle), y, radius * Math.sin(angle));
    }

    private static Vector rotateY(Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vector(v.getX() * cos - v.getZ() * sin, v.getY(), v.getX() * sin + v.getZ() * cos);
    }

    /** A named parametric shape. */
    public static final class Shape implements IdleShape {
        private final String id;
        private final BiFunction<Double, Integer, List<Vector>> fn;

        Shape(String id, BiFunction<Double, Integer, List<Vector>> fn) {
            this.id = id;
            this.fn = fn;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public List<Vector> points(double radius, int tick) {
            return fn.apply(radius, tick);
        }
    }
}
