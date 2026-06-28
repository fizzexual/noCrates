package com.nocrates.storage;

import com.nocrates.NoCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** Flat-file {@link DataStore}: one {@code playerdata/<uuid>.yml} per player. */
public final class YamlDataStore implements DataStore {

    private final NoCrates plugin;
    private final File folder;

    public YamlDataStore(NoCrates plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create playerdata folder.");
        }
    }

    @Override
    public CompletableFuture<PlayerData> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> loadSync(id));
    }

    private PlayerData loadSync(UUID id) {
        PlayerData data = new PlayerData(id);
        File file = new File(folder, id + ".yml");
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            read(config.getConfigurationSection("keys"), data.rawKeys());
            read(config.getConfigurationSection("opens"), data.rawOpens());
            read(config.getConfigurationSection("wins"), data.rawWins());
        }
        return data;
    }

    private void read(ConfigurationSection section, Map<String, Integer> into) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            into.put(key, section.getInt(key));
        }
    }

    @Override
    public void save(PlayerData data) {
        File file = new File(folder, data.uuid() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        write(config, "keys", data.rawKeys());
        write(config, "opens", data.rawOpens());
        write(config, "wins", data.rawWins());
        try {
            config.save(file);
            data.markClean();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player data " + file.getName(), e);
        }
    }

    private void write(YamlConfiguration config, String path, Map<String, Integer> map) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void close() {
    }
}
