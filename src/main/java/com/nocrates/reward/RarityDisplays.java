package com.nocrates.reward;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Rarity id -> MiniMessage display line, populated by the rarities module for previews. */
public final class RarityDisplays {

    private static final Map<String, String> DISPLAYS = new ConcurrentHashMap<>();

    private RarityDisplays() {
    }

    public static void put(String rarityId, String display) {
        DISPLAYS.put(rarityId.toLowerCase(Locale.ROOT), display);
    }

    public static String get(String rarityId) {
        return rarityId == null ? null : DISPLAYS.get(rarityId.toLowerCase(Locale.ROOT));
    }

    public static void clear() {
        DISPLAYS.clear();
    }
}
