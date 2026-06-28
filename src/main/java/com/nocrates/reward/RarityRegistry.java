package com.nocrates.reward;

import com.nocrates.NoCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Loads and serves {@link Rarity} definitions from {@code rarities.yml}. */
public final class RarityRegistry {

    private final Map<String, Rarity> rarities = new LinkedHashMap<>();
    private final Rarity fallback = new Rarity("common", "<white>", 1, false);

    public RarityRegistry(NoCrates plugin) {
        load(plugin);
    }

    public void load(NoCrates plugin) {
        File file = new File(plugin.getDataFolder(), "rarities.yml");
        if (!file.exists()) {
            plugin.saveResource("rarities.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        rarities.clear();
        ConfigurationSection section = config.getConfigurationSection("rarities");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                String id = key.toLowerCase(Locale.ROOT);
                rarities.put(id, new Rarity(
                        id,
                        entry.getString("color", "<white>"),
                        entry.getInt("order", 1),
                        entry.getBoolean("broadcast", false)));
            }
        }
    }

    public Rarity get(String id) {
        if (id == null) {
            return fallback;
        }
        return rarities.getOrDefault(id.toLowerCase(Locale.ROOT), fallback);
    }

    public Collection<Rarity> all() {
        return rarities.values();
    }
}
