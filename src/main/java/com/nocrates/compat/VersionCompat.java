package com.nocrates.compat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves {@link Material}, sounds and {@link Particle} by name across the
 * supported MC range (1.20 → 26.x) with graceful fallbacks, so a single jar
 * never hard-fails on a renamed or missing constant.
 *
 * <p>Sounds are played via the String-key API (e.g. {@code block.note_block.pling})
 * which is stable across versions even though the {@code Sound} type itself has
 * changed shape; callers may pass either dotted keys or {@code ENUM_STYLE} names.
 */
public final class VersionCompat {

    private VersionCompat() {
    }

    private static final Map<String, Material> MATERIAL_CACHE = new HashMap<>();
    private static final Map<String, Particle> PARTICLE_CACHE = new HashMap<>();

    /** Known particle renames between versions; tried as a fallback. */
    private static final Map<String, String> PARTICLE_ALIASES = new HashMap<>();

    static {
        PARTICLE_ALIASES.put("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        PARTICLE_ALIASES.put("HAPPY_VILLAGER", "VILLAGER_HAPPY");
        PARTICLE_ALIASES.put("SMOKE_NORMAL", "SMOKE");
        PARTICLE_ALIASES.put("SMOKE", "SMOKE_NORMAL");
        PARTICLE_ALIASES.put("REDSTONE", "DUST");
        PARTICLE_ALIASES.put("DUST", "REDSTONE");
        PARTICLE_ALIASES.put("FIREWORKS_SPARK", "FIREWORK");
        PARTICLE_ALIASES.put("FIREWORK", "FIREWORKS_SPARK");
    }

    public static Material material(String key, Material fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        Material cached = MATERIAL_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Material material = Material.matchMaterial(key);
        if (material == null) {
            try {
                material = Material.valueOf(key.toUpperCase(Locale.ROOT).replace("MINECRAFT:", ""));
            } catch (IllegalArgumentException ignored) {
                // unknown on this version
            }
        }
        if (material == null) {
            material = fallback;
        }
        MATERIAL_CACHE.put(key, material);
        return material;
    }

    public static Material material(String key) {
        return material(key, Material.STONE);
    }

    /** @return the particle, or {@code null} if it cannot be resolved (caller no-ops). */
    public static Particle particle(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String upper = key.toUpperCase(Locale.ROOT);
        if (PARTICLE_CACHE.containsKey(upper)) {
            return PARTICLE_CACHE.get(upper);
        }
        Particle particle = tryParticle(upper);
        if (particle == null) {
            String alias = PARTICLE_ALIASES.get(upper);
            if (alias != null) {
                particle = tryParticle(alias);
            }
        }
        PARTICLE_CACHE.put(upper, particle);
        return particle;
    }

    private static Particle tryParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Normalise an {@code ENUM_STYLE} or namespaced sound into a playable key. */
    public static String soundKey(String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        String value = spec.trim();
        if (value.contains(":") || value.contains(".")) {
            int idx = value.indexOf(':');
            return idx >= 0 ? value.substring(idx + 1) : value;
        }
        return value.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    public static void playSound(Player player, String spec, float volume, float pitch) {
        String key = soundKey(spec);
        if (key == null || player == null) {
            return;
        }
        try {
            player.playSound(player.getLocation(), key, volume, pitch);
        } catch (Throwable ignored) {
            // unknown sound on this version — silently skip
        }
    }

    public static void playSound(Location location, String spec, float volume, float pitch) {
        String key = soundKey(spec);
        if (key == null || location == null || location.getWorld() == null) {
            return;
        }
        try {
            location.getWorld().playSound(location, key, volume, pitch);
        } catch (Throwable ignored) {
            // unknown sound on this version — silently skip
        }
    }
}
