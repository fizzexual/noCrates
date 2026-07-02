package com.nocrates.module;

import com.nocrates.api.Addon;
import com.nocrates.api.AddonDescription;
import com.nocrates.api.NoCratesApi;
import com.nocrates.modules.animationsplus.AnimationsPlusModule;
import com.nocrates.modules.chesthunt.ChestHuntModule;
import com.nocrates.modules.crateclaim.CrateClaimModule;
import com.nocrates.modules.lastwinner.LastWinnerModule;
import com.nocrates.modules.lootboxes.LootboxModule;
import com.nocrates.modules.massopen.MassOpenModule;
import com.nocrates.modules.rarities.RaritiesModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Loads the built-in modules (toggleable in modules.yml, PhoenixCrates add-on parity)
 * and external addon jars from plugins/noCrates/addons/ (addon.yml + a main class
 * extending {@link Addon}). A failing addon is disabled and skipped, never fatal.
 */
public final class ModuleManager {

    private final JavaPlugin plugin;
    private final NoCratesApi api;
    private final List<Addon> enabled = new ArrayList<>();
    private final Map<String, Supplier<Addon>> builtins = new LinkedHashMap<>();
    private YamlConfiguration modulesYml = new YamlConfiguration();

    public ModuleManager(JavaPlugin plugin, NoCratesApi api) {
        this.plugin = plugin;
        this.api = api;
        builtins.put("rarities", RaritiesModule::new);
        builtins.put("last-winner", LastWinnerModule::new);
        builtins.put("crate-claim", CrateClaimModule::new);
        builtins.put("mass-opening", MassOpenModule::new);
        builtins.put("lootboxes", LootboxModule::new);
        builtins.put("animations-plus", AnimationsPlusModule::new);
        builtins.put("chest-hunt", ChestHuntModule::new);
    }

    public void enableAll() {
        File file = new File(plugin.getDataFolder(), "modules.yml");
        modulesYml = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        for (Map.Entry<String, Supplier<Addon>> entry : builtins.entrySet()) {
            String name = entry.getKey();
            if (!modulesYml.getBoolean("modules." + name, true)) {
                plugin.getLogger().info("Module '" + name + "' disabled in modules.yml.");
                continue;
            }
            enable(entry.getValue().get(),
                    new AddonDescription(name, "", plugin.getDescription().getVersion(), "noCrates", true));
        }
        loadExternal();
        plugin.getLogger().info("Enabled " + enabled.size() + " module(s)/addon(s).");
    }

    private void loadExternal() {
        File dir = new File(plugin.getDataFolder(), "addons");
        dir.mkdirs();
        File[] jars = dir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars == null) return;
        for (File jar : jars) {
            try {
                loadAddonJar(jar);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not load addon " + jar.getName() + ": " + e.getMessage());
            }
        }
    }

    private void loadAddonJar(File jar) throws Exception {
        YamlConfiguration description;
        try (JarFile jarFile = new JarFile(jar)) {
            var entry = jarFile.getJarEntry("addon.yml");
            if (entry == null) {
                plugin.getLogger().warning("Addon " + jar.getName() + " has no addon.yml — skipped.");
                return;
            }
            try (var in = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                description = YamlConfiguration.loadConfiguration(in);
            }
        }
        String name = description.getString("name");
        String main = description.getString("main");
        if (name == null || main == null) {
            plugin.getLogger().warning("Addon " + jar.getName() + ": addon.yml needs 'name' and 'main'.");
            return;
        }
        AddonClassLoader loader = new AddonClassLoader(jar.toURI().toURL(), plugin.getClass().getClassLoader());
        Class<?> mainClass = Class.forName(main, true, loader);
        if (!Addon.class.isAssignableFrom(mainClass)) {
            plugin.getLogger().warning("Addon " + name + ": main class does not extend Addon.");
            return;
        }
        Addon addon = (Addon) mainClass.getDeclaredConstructor().newInstance();
        enable(addon, new AddonDescription(name, main,
                description.getString("version", "1.0"), description.getString("author", "unknown"), false));
    }

    private void enable(Addon addon, AddonDescription description) {
        try {
            ConfigurationSection config = modulesYml.getConfigurationSection(description.name());
            if (config == null) config = new MemoryConfiguration();
            addon.initialize(api, description, Logger.getLogger("noCrates/" + description.name()), config);
            addon.onLoad();
            addon.onEnable();
            enabled.add(addon);
        } catch (Throwable t) {
            plugin.getLogger().severe("Addon '" + description.name() + "' failed to enable: " + t);
        }
    }

    public void disableAll() {
        for (int i = enabled.size() - 1; i >= 0; i--) {
            try {
                enabled.get(i).onDisable();
            } catch (Throwable t) {
                plugin.getLogger().warning("Addon '" + enabled.get(i).description().name()
                        + "' failed to disable: " + t.getMessage());
            }
        }
        enabled.clear();
    }

    public List<Addon> enabled() {
        return List.copyOf(enabled);
    }
}
