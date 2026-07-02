package com.nocrates.animation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Small math toolkit shared by the choreographed animations. */
public final class Curves {

    private Curves() {
    }

    /** {@code n} points on a horizontal ring of {@code radius}, rotated by {@code phase}. */
    public static List<Vector> ring(double radius, int n, double phase) {
        List<Vector> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double a = phase + i * 2 * Math.PI / n;
            out.add(new Vector(radius * Math.cos(a), 0, radius * Math.sin(a)));
        }
        return out;
    }

    /** Evenly spaced points from a to b (inclusive endpoints). */
    public static List<Location> line(Location a, Location b, int steps) {
        List<Location> out = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double f = i / (double) Math.max(1, steps);
            out.add(a.clone().add(
                    (b.getX() - a.getX()) * f,
                    (b.getY() - a.getY()) * f,
                    (b.getZ() - a.getZ()) * f));
        }
        return out;
    }

    public static Vector rotateY(Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vector(v.getX() * cos - v.getZ() * sin, v.getY(), v.getX() * sin + v.getZ() * cos);
    }

    /** Hue (0-1) to a Bukkit color, full saturation/value. */
    public static Color hue(double h) {
        java.awt.Color awt = java.awt.Color.getHSBColor((float) (h % 1.0), 0.9f, 1f);
        return Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    /** Smooth 0→1 ease. */
    public static double ease(double t) {
        double x = Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }
}
