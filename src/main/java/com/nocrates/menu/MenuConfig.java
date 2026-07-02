package com.nocrates.menu;

import com.nocrates.core.Reloadable;
import com.nocrates.item.ItemSpec;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads menus/*.yml into reusable specs so server owners can restyle every GUI
 * (titles, rows, decorative items, functional slots, click actions).
 */
public final class MenuConfig implements Reloadable {

    public static final String[] DEFAULTS = {
            "preview", "confirmation", "selective", "reroll", "virtualkeys", "massopen"
    };

    public record Icon(List<Integer> slots, ItemSpec item, List<String> actions) {
    }

    public record Spec(String id, String title, int rows, List<Integer> contentSlots, Map<String, Icon> icons) {

        public Icon icon(String key) {
            return icons.get(key);
        }
    }

    private final JavaPlugin plugin;
    private final Map<String, Spec> menus = new HashMap<>();

    public MenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        menus.clear();
        File dir = new File(plugin.getDataFolder(), "menus");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            menus.put(id, parse(id, YamlConfiguration.loadConfiguration(file)));
        }
    }

    private Spec parse(String id, ConfigurationSection c) {
        String title = c.getString("title", id);
        int rows = Math.max(1, Math.min(6, c.getInt("rows", 6)));
        List<Integer> content = c.getIntegerList("content-slots");
        Map<String, Icon> icons = new HashMap<>();
        ConfigurationSection items = c.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;
                List<Integer> slots = item.isList("slots") ? item.getIntegerList("slots")
                        : List.of(item.getInt("slot", 0));
                ItemSpec spec = ItemSpec.fromConfig(item.getConfigurationSection("item"));
                icons.put(key, new Icon(slots, spec, new ArrayList<>(item.getStringList("actions"))));
            }
        }
        return new Spec(id, title, rows, content, icons);
    }

    /** Never null: unknown ids get a plain 6-row fallback so menus keep working. */
    public Spec get(String id) {
        Spec spec = menus.get(id);
        if (spec != null) return spec;
        return new Spec(id, id, 6, List.of(), Map.of());
    }
}
