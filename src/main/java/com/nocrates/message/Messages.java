package com.nocrates.message;

import com.nocrates.NoCrates;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code messages.yml} and renders entries with MiniMessage. All
 * player-facing text flows through here so servers can fully restyle the
 * plugin. Use {@link #ph(String, String)} to build placeholder resolvers.
 */
public final class Messages {

    private final NoCrates plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> values = new HashMap<>();
    private String prefix = "";

    public Messages(NoCrates plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        values.clear();
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                values.put(key, config.getString(key));
            }
        }
        prefix = values.getOrDefault("prefix", "");
    }

    /** Render a message (no prefix) as a component, with placeholders applied. */
    public Component component(String key, TagResolver... placeholders) {
        return miniMessage.deserialize(values.getOrDefault(key, key), placeholders);
    }

    /** Send a prefixed message to a sender. Missing/empty keys are skipped. */
    public void send(CommandSender to, String key, TagResolver... placeholders) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            return;
        }
        to.sendMessage(miniMessage.deserialize(prefix + value, placeholders));
    }

    /** Send a message without the prefix (e.g. for multi-line content). */
    public void sendRaw(CommandSender to, String key, TagResolver... placeholders) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            return;
        }
        to.sendMessage(miniMessage.deserialize(value, placeholders));
    }

    public String rawValue(String key) {
        return values.get(key);
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    /** Convenience for an unparsed (literal) placeholder, e.g. {@code <crate>}. */
    public static TagResolver ph(String name, String value) {
        return Placeholder.unparsed(name, value == null ? "" : value);
    }
}
