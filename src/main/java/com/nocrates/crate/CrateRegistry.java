package com.nocrates.crate;

import com.nocrates.core.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Loads crates/*.yml, keeps them by id, and writes edits back to their files. */
public final class CrateRegistry implements Reloadable {

    private final Plugin plugin;
    private final Map<String, Crate> crates = new LinkedHashMap<>();
    private final File dir;

    public CrateRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "crates");
        reload();
    }

    @Override
    public void reload() {
        crates.clear();
        dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            try {
                crates.put(id, CrateSerializer.read(id, YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                plugin.getLogger().severe("Could not load crate " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + crates.size() + " crate(s).");
    }

    public Crate get(String id) {
        return id == null ? null : crates.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Crate> all() {
        return crates.values();
    }

    public java.util.Set<String> ids() {
        return crates.keySet();
    }

    public boolean exists(String id) {
        return id != null && crates.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Crate create(String id) {
        Crate crate = new Crate(id);
        crates.put(crate.id(), crate);
        save(crate);
        return crate;
    }

    /** Registers an externally built crate (importers) and writes its file. */
    public void put(Crate crate) {
        crates.put(crate.id(), crate);
        save(crate);
    }

    public Crate clone(Crate source, String newId) {
        YamlConfiguration yml = new YamlConfiguration();
        CrateSerializer.write(source, yml);
        Crate copy = CrateSerializer.read(newId.toLowerCase(Locale.ROOT), yml);
        copy.locations().clear(); // placements are never cloned
        crates.put(copy.id(), copy);
        save(copy);
        return copy;
    }

    public void delete(String id) {
        Crate removed = crates.remove(id.toLowerCase(Locale.ROOT));
        if (removed != null) {
            File file = fileOf(removed.id());
            if (file.isFile() && !file.delete()) {
                plugin.getLogger().warning("Could not delete " + file.getName());
            }
        }
    }

    public void save(Crate crate) {
        YamlConfiguration yml = new YamlConfiguration();
        CrateSerializer.write(crate, yml);
        try {
            yml.save(fileOf(crate.id()));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save crate " + crate.id() + ": " + e.getMessage());
        }
    }

    private File fileOf(String id) {
        return new File(dir, id + ".yml");
    }
}
