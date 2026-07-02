package com.nocrates.compat;

import org.bukkit.Bukkit;

/**
 * Parses the running server version once. Handles both classic ("1.20.1-R0.1-SNAPSHOT")
 * and 2026 year-based ("26.2-R0.1-SNAPSHOT" / "26.2.1") version strings; year-based
 * versions sort above every 1.x because the leading number jumped from 1 to 26.
 */
public final class ServerVersion {

    private static final int[] CURRENT = parse(Bukkit.getBukkitVersion());

    private ServerVersion() {
    }

    public static int[] current() {
        return CURRENT.clone();
    }

    public static String display() {
        return CURRENT[0] + "." + CURRENT[1] + (CURRENT[2] > 0 ? "." + CURRENT[2] : "");
    }

    /** True when the server is at least the given version string ("1.21", "26.1", ...). */
    public static boolean atLeast(String version) {
        return compare(Bukkit.getBukkitVersion(), version) >= 0;
    }

    /** Compares two Minecraft version strings; year-based versions compare above 1.x naturally. */
    public static int compare(String a, String b) {
        int[] va = parse(a), vb = parse(b);
        for (int i = 0; i < 3; i++) {
            int d = Integer.compare(va[i], vb[i]);
            if (d != 0) return d;
        }
        return 0;
    }

    static int[] parse(String raw) {
        String s = raw == null ? "" : raw.trim();
        int dash = s.indexOf('-');
        if (dash > 0) s = s.substring(0, dash);
        String[] parts = s.split("\\.");
        int[] out = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
                out[i] = 0;
            }
        }
        return out;
    }
}
