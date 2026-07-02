package com.nocrates.item;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Custom-textured player heads via the portable PlayerProfile API (1.18+). */
public final class Heads {

    private static final Pattern URL_IN_JSON = Pattern.compile("\"url\"\\s*:\\s*\"(http[^\"]+)\"");

    private Heads() {
    }

    /** Applies a base64 textures value (the classic {@code textures.minecraft.net} payload). */
    public static void applyTexture(ItemStack head, String base64) {
        if (!(head.getItemMeta() instanceof SkullMeta meta)) return;
        String url = extractUrl(base64);
        if (url == null) return;
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)), "noCrates");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    static String extractUrl(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            Matcher m = URL_IN_JSON.matcher(json);
            return m.find() ? m.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
