package com.nocrates.compat;

import org.bukkit.Bukkit;

/**
 * Parses the running server version once. Used only for coarse feature gating;
 * noCrates avoids NMS so most code is version-agnostic.
 *
 * <p>{@link #feature()} encodes the first two version components into a sortable
 * int: {@code 1.20 -> 1020}, {@code 1.21 -> 1021}, {@code 26.1 -> 26001}.
 */
public final class ServerVersion {

    private static final String RAW;
    private static final int FEATURE;

    static {
        String raw = "1.20";
        try {
            raw = Bukkit.getBukkitVersion().split("-")[0];
        } catch (Throwable ignored) {
            // not running on a live server (e.g. unit context) — keep default
        }
        RAW = raw;
        FEATURE = computeFeature(raw);
    }

    private ServerVersion() {
    }

    private static int computeFeature(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major * 1000 + minor;
        } catch (Exception e) {
            return 1020;
        }
    }

    public static String raw() {
        return RAW;
    }

    public static int feature() {
        return FEATURE;
    }
}
