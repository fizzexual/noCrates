package com.nocrates.key;

import com.nocrates.core.Reloadable;
import com.nocrates.item.ItemSpec;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** keys.yml round-trip: key id -> item spec + virtual flag. */
public final class KeyRegistry implements Reloadable {

    private final Plugin plugin;
    private final Map<String, Key> keys = new LinkedHashMap<>();

    public KeyRegistry(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        keys.clear();
        File file = new File(plugin.getDataFolder(), "keys.yml");
        if (!file.isFile()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("keys");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            ItemSpec item = ItemSpec.fromConfig(s.getConfigurationSection("item"));
            keys.put(id.toLowerCase(Locale.ROOT), new Key(id.toLowerCase(Locale.ROOT), item, s.getBoolean("virtual", false)));
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Key key : keys.values()) {
            ConfigurationSection s = yml.createSection("keys." + key.id());
            s.set("virtual", key.virtual());
            key.item().write(s.createSection("item"));
        }
        try {
            yml.save(new File(plugin.getDataFolder(), "keys.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save keys.yml: " + e.getMessage());
        }
    }

    public Key get(String id) {
        return id == null ? null : keys.get(id.toLowerCase(Locale.ROOT));
    }

    public Key getOrCreate(String id) {
        return keys.computeIfAbsent(id.toLowerCase(Locale.ROOT), k -> {
            ItemSpec item = new ItemSpec("TRIPWIRE_HOOK")
                    .name("<yellow>" + capitalize(k) + " Key")
                    .glow(true);
            return new Key(k, item, false);
        });
    }

    public void remove(String id) {
        keys.remove(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Key> all() {
        return keys.values();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
