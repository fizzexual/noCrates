package com.nocrates.animation;

import java.util.Locale;

/**
 * Idle effect definition string, PhoenixCrates-style:
 * {@code SHAPE;{PARTICLE;#hex;offX;offY;offZ;radius;velocity;amount}}
 * The color is optional (colorable particles only). Example:
 * {@code SPIRAL;{DUST;#7b5cff;0;0.2;0;1.2;0.05;2}}
 */
public record EffectSpec(String shape, String particle, String hexColor,
                         double offX, double offY, double offZ,
                         double radius, double velocity, int amount) {

    public static EffectSpec parse(String line) {
        if (line == null) throw new IllegalArgumentException("null effect spec");
        String s = line.trim();
        int semi = s.indexOf(';');
        if (semi <= 0) throw new IllegalArgumentException("Missing shape in effect spec: " + line);
        String shape = s.substring(0, semi).trim().toUpperCase(Locale.ROOT);
        String rest = s.substring(semi + 1).trim();
        if (!rest.startsWith("{") || !rest.endsWith("}")) {
            throw new IllegalArgumentException("Expected {...} block in effect spec: " + line);
        }
        String[] parts = rest.substring(1, rest.length() - 1).split(";", -1);
        if (parts.length < 1 || parts[0].isBlank()) {
            throw new IllegalArgumentException("Missing particle in effect spec: " + line);
        }
        String particle = parts[0].trim().toUpperCase(Locale.ROOT);
        String hex = parts.length > 1 ? parts[1].trim() : "";
        if (!hex.isEmpty() && !hex.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Bad hex color '" + hex + "' in effect spec: " + line);
        }
        return new EffectSpec(shape, particle, hex.isEmpty() ? null : hex,
                num(parts, 2), num(parts, 3), num(parts, 4),
                parts.length > 5 && !parts[5].isBlank() ? num(parts, 5) : 1.0,
                num(parts, 6),
                parts.length > 7 && !parts[7].isBlank() ? (int) num(parts, 7) : 1);
    }

    private static double num(String[] parts, int index) {
        if (index >= parts.length || parts[index].isBlank()) return 0;
        try {
            return Double.parseDouble(parts[index].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number '" + parts[index] + "' in effect spec");
        }
    }

    public String serialize() {
        return shape + ";{" + particle + ";" + (hexColor == null ? "" : hexColor) + ";"
                + trim(offX) + ";" + trim(offY) + ";" + trim(offZ) + ";"
                + trim(radius) + ";" + trim(velocity) + ";" + amount + "}";
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    public org.bukkit.Color bukkitColor() {
        if (hexColor == null) return org.bukkit.Color.WHITE;
        int rgb = Integer.parseInt(hexColor.substring(1), 16);
        return org.bukkit.Color.fromRGB(rgb);
    }
}
