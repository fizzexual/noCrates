package com.nocrates.crate;

import com.nocrates.NoCrates;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** Loads, stores and persists {@link Crate}s from the {@code crates/} folder. */
public final class CrateRegistry {

    private final NoCrates plugin;
    private final File folder;
    private final Map<String, Crate> crates = new LinkedHashMap<>();

    public CrateRegistry(NoCrates plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "crates");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create crates folder.");
        }
    }

    public void loadAll() {
        crates.clear();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.saveResource("crates/example.yml", false);
            files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        }
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            String name = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                crates.put(name, CrateSerializer.read(name, config));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load crate '" + name + "'", e);
            }
        }
        plugin.getLogger().info("Loaded " + crates.size() + " crate(s).");
    }

    public void save(Crate crate) {
        File file = new File(folder, crate.name() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        CrateSerializer.write(crate, config);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save crate '" + crate.name() + "'", e);
        }
        crates.put(crate.name().toLowerCase(Locale.ROOT), crate);
    }

    public void remove(String name) {
        if (name == null) {
            return;
        }
        crates.remove(name.toLowerCase(Locale.ROOT));
        File file = new File(folder, name.toLowerCase(Locale.ROOT) + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete crate file " + file.getName());
        }
    }

    public Crate get(String name) {
        return name == null ? null : crates.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return name != null && crates.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Crate> all() {
        return crates.values();
    }

    public Set<String> names() {
        return crates.keySet();
    }

    public void reload() {
        loadAll();
    }
}
