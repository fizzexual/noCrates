package com.nocrates.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Player-facing text. Loads languages/&lt;code&gt;.yml from the data folder, falling back
 * key-by-key to the file shipped in the jar and finally to embedded en_US. An empty
 * string value disables that message entirely.
 */
public final class Lang {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private YamlConfiguration active = new YamlConfiguration();
    private YamlConfiguration jarSame = new YamlConfiguration();
    private YamlConfiguration jarEnglish = new YamlConfiguration();
    private String prefix = "";

    public Lang(JavaPlugin plugin, String code) {
        this.plugin = plugin;
        load(code);
    }

    public void load(String code) {
        File file = new File(plugin.getDataFolder(), "languages/" + code + ".yml");
        this.active = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        this.jarSame = fromJar("languages/" + code + ".yml");
        this.jarEnglish = fromJar("languages/en_US.yml");
        this.prefix = rawString("prefix");
    }

    private YamlConfiguration fromJar(String path) {
        InputStream in = plugin.getResource(path);
        if (in == null) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Raw configured string ("" when the message is disabled; key itself when unknown). */
    public String rawString(String key) {
        String v = active.getString(key);
        if (v == null) v = jarSame.getString(key);
        if (v == null) v = jarEnglish.getString(key);
        return v == null ? key : v;
    }

    public Component raw(String key, TagResolver... tags) {
        String s = rawString(key);
        if (s.isEmpty()) return Component.empty();
        try {
            return MM.deserialize(Text.legacyToMini(s), tags);
        } catch (Exception e) {
            return Component.text(s);
        }
    }

    /** Message with the configured prefix prepended. */
    public Component msg(String key, TagResolver... tags) {
        String s = rawString(key);
        if (s.isEmpty()) return Component.empty();
        try {
            return MM.deserialize(Text.legacyToMini(prefix + s), tags);
        } catch (Exception e) {
            return Component.text(s);
        }
    }

    /** Sends the prefixed message unless it is configured empty. */
    public void send(CommandSender to, String key, TagResolver... tags) {
        if (rawString(key).isEmpty()) return;
        to.sendMessage(msg(key, tags));
    }

    public void sendRaw(CommandSender to, String key, TagResolver... tags) {
        if (rawString(key).isEmpty()) return;
        to.sendMessage(raw(key, tags));
    }
}
