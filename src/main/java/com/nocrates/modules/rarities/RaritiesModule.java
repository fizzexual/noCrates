package com.nocrates.modules.rarities;

import com.nocrates.api.Addon;
import com.nocrates.api.events.CrateOpenEvent;
import com.nocrates.core.Services;
import com.nocrates.key.Key;
import com.nocrates.reward.RarityDisplays;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RollEngine;
import com.nocrates.reward.Weights;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rarities (PhoenixCrates "Rarity" add-on parity): rarity tiers with optional
 * synchronized drop rates, rarity lines in previews, and keys that guarantee a reward
 * of specific rarities.
 */
public final class RaritiesModule extends Addon implements Listener {

    private record Rarity(String id, String display, double syncPercentage, boolean enabled) {
    }

    private final Map<String, Rarity> rarities = new ConcurrentHashMap<>();
    private final RollEngine roller = new RollEngine(new Random());

    @Override
    public void onEnable() {
        ((com.nocrates.NoCratesPlugin) api().plugin()).saveDefaultResource("rarities.yml");
        load();
        Weights.resolver(this::weightOf);
        Bukkit.getPluginManager().registerEvents(this, api().plugin());
        Services.get().reloads().register(this::load);
    }

    @Override
    public void onDisable() {
        Weights.resolver(null);
        RarityDisplays.clear();
    }

    private void load() {
        rarities.clear();
        RarityDisplays.clear();
        File file = new File(api().plugin().getDataFolder(), "rarities.yml");
        if (!file.isFile()) return;
        ConfigurationSection root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("rarities");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Rarity rarity = new Rarity(id.toLowerCase(Locale.ROOT),
                    s.getString("display", id),
                    s.getDouble("sync-percentage", -1),
                    s.getBoolean("enabled", true));
            rarities.put(rarity.id(), rarity);
            RarityDisplays.put(rarity.id(), rarity.display());
        }
        logger().info("Loaded " + rarities.size() + " rarities.");
    }

    private double weightOf(Reward reward) {
        if (reward.rarity() != null) {
            Rarity rarity = rarities.get(reward.rarity().toLowerCase(Locale.ROOT));
            if (rarity != null && rarity.enabled() && rarity.syncPercentage() >= 0) {
                return rarity.syncPercentage();
            }
        }
        return reward.percentage();
    }

    /** Key rarity guarantees: force the outcome into a guaranteed tier when needed. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(CrateOpenEvent event) {
        if (event.crate().keys().isEmpty() || event.outcome().isEmpty()) return;
        Key key = api().keys().get(event.crate().keys().get(0).keyId());
        if (key == null || key.guaranteeRarities().isEmpty()) return;
        List<String> guaranteed = key.guaranteeRarities().stream()
                .map(r -> r.toLowerCase(Locale.ROOT)).toList();
        Reward current = event.outcome().get(0);
        if (current.rarity() != null && guaranteed.contains(current.rarity().toLowerCase(Locale.ROOT))) return;
        List<Reward> candidates = new ArrayList<>();
        for (Reward reward : event.crate().rewards().values()) {
            if (reward.rarity() != null && guaranteed.contains(reward.rarity().toLowerCase(Locale.ROOT))
                    && Weights.of(reward) > 0) {
                candidates.add(reward);
            }
        }
        if (candidates.isEmpty()) return;
        Reward forced = roller.roll(candidates, Weights::of);
        if (forced != null) event.outcome().set(0, forced);
    }
}
