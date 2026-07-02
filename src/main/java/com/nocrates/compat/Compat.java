package com.nocrates.compat;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version-tolerant resolution of materials, particles and sounds by name.
 *
 * The 1.20.5 update renamed many Particle constants and 1.21.3 turned Sound into an
 * interface, so this class never references renamed constants directly: particles are
 * resolved reflectively through a rename chain and sounds are played through the
 * String-based playSound API (stable since 1.7). Results are cached.
 */
public final class Compat {

    /** old-name <-> new-name pairs tried in both directions. */
    private static final String[][] PARTICLE_RENAMES = {
            {"REDSTONE", "DUST"},
            {"VILLAGER_HAPPY", "HAPPY_VILLAGER"},
            {"VILLAGER_ANGRY", "ANGRY_VILLAGER"},
            {"SPELL_MOB", "ENTITY_EFFECT"},
            {"SPELL_WITCH", "WITCH"},
            {"SPELL", "EFFECT"},
            {"SPELL_INSTANT", "INSTANT_EFFECT"},
            {"EXPLOSION_NORMAL", "POOF"},
            {"EXPLOSION_LARGE", "EXPLOSION"},
            {"EXPLOSION_HUGE", "EXPLOSION_EMITTER"},
            {"SMOKE_NORMAL", "SMOKE"},
            {"SMOKE_LARGE", "LARGE_SMOKE"},
            {"FIREWORKS_SPARK", "FIREWORK"},
            {"ENCHANTMENT_TABLE", "ENCHANT"},
            {"ITEM_CRACK", "ITEM"},
            {"BLOCK_CRACK", "BLOCK"},
            {"BLOCK_DUST", "BLOCK"},
            {"WATER_DROP", "RAIN"},
            {"WATER_SPLASH", "SPLASH"},
            {"WATER_BUBBLE", "BUBBLE"},
            {"WATER_WAKE", "BUBBLE_POP"},
            {"TOTEM", "TOTEM_OF_UNDYING"},
            {"SNOWBALL", "ITEM_SNOWBALL"},
            {"SLIME", "ITEM_SLIME"},
            {"MOB_APPEARANCE", "ELDER_GUARDIAN"},
            {"TOWN_AURA", "MYCELIUM"},
            {"CRIT_MAGIC", "ENCHANTED_HIT"},
            {"DRIP_WATER", "DRIPPING_WATER"},
            {"DRIP_LAVA", "DRIPPING_LAVA"}
    };

    private static final Map<String, Particle> PARTICLES = new ConcurrentHashMap<>();
    private static final Map<String, Material> MATERIALS = new ConcurrentHashMap<>();
    private static final Particle NULL_PARTICLE = null;
    private static Method particleValueOf;

    private Compat() {
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.isEmpty()) return fallback;
        Material cached = MATERIALS.get(name);
        if (cached != null) return cached;
        Material m = Material.matchMaterial(name.trim());
        if (m == null) m = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        if (m == null) return fallback;
        MATERIALS.put(name, m);
        return m;
    }

    /** Resolves a particle by any historical name; null when nothing matches on this server. */
    public static Particle particle(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        Particle cached = PARTICLES.get(key);
        if (cached != null) return cached;
        Particle p = particleByName(key);
        if (p == null) {
            for (String[] pair : PARTICLE_RENAMES) {
                if (pair[0].equals(key)) p = particleByName(pair[1]);
                else if (pair[1].equals(key)) p = particleByName(pair[0]);
                if (p != null) break;
            }
        }
        if (p != null) PARTICLES.put(key, p);
        return p;
    }

    private static Particle particleByName(String name) {
        try {
            if (particleValueOf == null) particleValueOf = Particle.class.getMethod("valueOf", String.class);
            return (Particle) particleValueOf.invoke(null, name);
        } catch (ReflectiveOperationException ignored) {
            return NULL_PARTICLE;
        }
    }

    /** True when the resolved particle takes DustOptions (colorable dust). */
    public static boolean isDust(Particle particle) {
        return particle != null && particle.getDataType() == Particle.DustOptions.class;
    }

    public static void spawnDust(Location at, Color color, float size, int count,
                                 double ox, double oy, double oz) {
        Particle dust = particle("DUST");
        if (dust == null || at.getWorld() == null) return;
        at.getWorld().spawnParticle(dust, at, count, ox, oy, oz, 0, new Particle.DustOptions(color, size));
    }

    public static void spawn(Particle particle, Location at, int count,
                             double ox, double oy, double oz, double speed) {
        if (particle == null || at.getWorld() == null) return;
        Object data = null;
        Class<?> type = particle.getDataType();
        if (type == Particle.DustOptions.class) {
            data = new Particle.DustOptions(Color.WHITE, 1.0f);
        } else if (type != Void.class) {
            return; // particles needing block/item data are not used by effect specs
        }
        at.getWorld().spawnParticle(particle, at, count, ox, oy, oz, speed, data);
    }

    /**
     * Plays a sound by enum-style ("ENTITY_PLAYER_LEVELUP") or key-style
     * ("entity.player.levelup" / "minecraft:x.y") name via the String playSound API,
     * which is stable across every supported version.
     */
    public static void play(Player p, String sound, float volume, float pitch) {
        if (p == null || sound == null || sound.isEmpty()) return;
        p.playSound(p.getLocation(), soundKey(sound), volume, pitch);
    }

    public static void playAt(Location at, String sound, float volume, float pitch) {
        if (at == null || at.getWorld() == null || sound == null || sound.isEmpty()) return;
        at.getWorld().playSound(at, soundKey(sound), volume, pitch);
    }

    private static final Map<String, String> SOUND_KEYS = new ConcurrentHashMap<>();

    /**
     * Enum-style names resolve through the Sound registry (reflective, so the 1.21.3
     * enum→interface change can't break us) because vanilla keys keep underscores
     * inside segments (block.note_block.chime) — naive '_'→'.' conversion is wrong.
     */
    static String soundKey(String sound) {
        String s = sound.trim();
        if (s.indexOf(':') >= 0 || s.indexOf('.') >= 0) return s.toLowerCase(Locale.ROOT);
        String upper = s.toUpperCase(Locale.ROOT);
        return SOUND_KEYS.computeIfAbsent(upper, name -> {
            try {
                Class<?> soundClass = Class.forName("org.bukkit.Sound");
                Object value = soundClass.getMethod("valueOf", String.class).invoke(null, name);
                Object key = soundClass.getMethod("getKey").invoke(value);
                return key.toString();
            } catch (ReflectiveOperationException e) {
                // unknown enum name: last-resort conversion (correct for single-word segments)
                return name.toLowerCase(Locale.ROOT).replace('_', '.');
            }
        });
    }

    private static final String[][] POTION_RENAMES = {
            {"SLOW", "SLOWNESS"}, {"FAST_DIGGING", "HASTE"}, {"SLOW_DIGGING", "MINING_FATIGUE"},
            {"INCREASE_DAMAGE", "STRENGTH"}, {"HEAL", "INSTANT_HEALTH"}, {"HARM", "INSTANT_DAMAGE"},
            {"JUMP", "JUMP_BOOST"}, {"CONFUSION", "NAUSEA"}, {"DAMAGE_RESISTANCE", "RESISTANCE"}
    };

    /** Potion effect by old or new name; null when unresolvable. */
    @SuppressWarnings("deprecation")
    public static org.bukkit.potion.PotionEffectType potionEffect(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.trim().toUpperCase(Locale.ROOT);
        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(key);
        if (type != null) return type;
        for (String[] pair : POTION_RENAMES) {
            if (pair[0].equals(key)) type = org.bukkit.potion.PotionEffectType.getByName(pair[1]);
            else if (pair[1].equals(key)) type = org.bukkit.potion.PotionEffectType.getByName(pair[0]);
            if (type != null) return type;
        }
        return null;
    }
}
