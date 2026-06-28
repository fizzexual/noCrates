package com.nocrates.crate;

import com.nocrates.key.KeyType;
import com.nocrates.reward.DisplaySpec;
import com.nocrates.reward.Pity;
import com.nocrates.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Converts between a {@link Crate} and a YAML {@link ConfigurationSection}.
 * {@code write} then {@code read} reproduces an equivalent crate (round-trip),
 * which is verified by unit test.
 */
public final class CrateSerializer {

    private CrateSerializer() {
    }

    public static Crate read(String name, ConfigurationSection c) {
        Crate.Builder builder = Crate.builder(name)
                .displayName(c.getString("display-name", name))
                .animation(c.getString("animation", "reveal"));

        ConfigurationSection keySection = c.getConfigurationSection("key");
        if (keySection != null) {
            KeyType type = KeyType.from(keySection.getString("type", "virtual"));
            String keyId = keySection.getString("id", name);
            DisplaySpec item = readDisplay(keySection.getConfigurationSection("item"));
            builder.key(new CrateKeySpec(type, keyId, item));
        }

        ConfigurationSection blockSection = c.getConfigurationSection("block");
        if (blockSection != null) {
            builder.block(new CrateBlock(
                    blockSection.getBoolean("enabled", true),
                    blockSection.getStringList("locations"),
                    blockSection.getStringList("hologram"),
                    blockSection.getString("particle")));
        }

        ConfigurationSection previewSection = c.getConfigurationSection("preview");
        if (previewSection != null) {
            builder.preview(previewSection.getBoolean("enabled", true),
                    previewSection.getString("title", name + " — Rewards"));
        }

        ConfigurationSection pitySection = c.getConfigurationSection("pity");
        if (pitySection != null) {
            builder.pity(new Pity(
                    pitySection.getBoolean("enabled", false),
                    pitySection.getInt("every", 0),
                    pitySection.getString("tier")));
        }

        ConfigurationSection settings = c.getConfigurationSection("settings");
        if (settings != null) {
            builder.broadcast(settings.getBoolean("broadcast", false));
            builder.cooldownSeconds(settings.getInt("cooldown-seconds", 0));
            builder.openSound(settings.getString("open-sound"));
        }

        ConfigurationSection chestHunt = c.getConfigurationSection("chesthunt");
        if (chestHunt != null) {
            builder.chestHunt(new ChestHuntSettings(
                    chestHunt.getInt("chests", 8),
                    chestHunt.getInt("picks", 4),
                    chestHunt.getInt("radius", 2),
                    chestHunt.getInt("timeout-seconds", 30)));
        }

        ConfigurationSection rewards = c.getConfigurationSection("rewards");
        if (rewards != null) {
            for (String id : rewards.getKeys(false)) {
                ConfigurationSection r = rewards.getConfigurationSection(id);
                if (r == null) {
                    continue;
                }
                builder.addReward(new Reward(
                        id,
                        r.getString("rarity", "common"),
                        r.getDouble("chance", 1.0),
                        readDisplay(r.getConfigurationSection("display")),
                        r.getStringList("actions"),
                        r.getInt("max-per-player", -1),
                        r.getInt("cooldown-seconds", 0)));
            }
        }

        return builder.build();
    }

    public static void write(Crate crate, ConfigurationSection c) {
        c.set("display-name", crate.displayName());
        c.set("animation", crate.animation());

        c.set("key.type", crate.key().type().name().toLowerCase());
        c.set("key.id", crate.key().keyId());
        if (crate.key().item() != null) {
            writeDisplay(c, "key.item", crate.key().item());
        }

        CrateBlock block = crate.block();
        if (block != null) {
            c.set("block.enabled", block.isEnabled());
            c.set("block.locations", block.locations());
            c.set("block.hologram", block.hologram());
            if (block.particle() != null) {
                c.set("block.particle", block.particle());
            }
        }

        c.set("preview.enabled", crate.previewEnabled());
        c.set("preview.title", crate.previewTitle());

        c.set("pity.enabled", crate.pity().enabled());
        c.set("pity.every", crate.pity().every());
        if (crate.pity().tier() != null) {
            c.set("pity.tier", crate.pity().tier());
        }

        c.set("settings.broadcast", crate.broadcast());
        c.set("settings.cooldown-seconds", crate.cooldownSeconds());
        if (crate.openSound() != null) {
            c.set("settings.open-sound", crate.openSound());
        }

        if (crate.animation().equalsIgnoreCase("chesthunt")) {
            ChestHuntSettings ch = crate.chestHunt();
            c.set("chesthunt.chests", ch.chests());
            c.set("chesthunt.picks", ch.picks());
            c.set("chesthunt.radius", ch.radius());
            c.set("chesthunt.timeout-seconds", ch.timeoutSeconds());
        }

        for (Reward reward : crate.rewards()) {
            String base = "rewards." + reward.id();
            c.set(base + ".rarity", reward.rarityId());
            c.set(base + ".chance", reward.weight());
            if (reward.display() != null) {
                writeDisplay(c, base + ".display", reward.display());
            }
            c.set(base + ".actions", reward.actions());
            c.set(base + ".max-per-player", reward.maxPerPlayer());
            c.set(base + ".cooldown-seconds", reward.cooldownSeconds());
        }
    }

    private static DisplaySpec readDisplay(ConfigurationSection d) {
        if (d == null) {
            return null;
        }
        Integer customModelData = d.contains("custom-model-data") ? d.getInt("custom-model-data") : null;
        return new DisplaySpec(
                d.getString("material", "STONE"),
                d.getString("name"),
                d.getStringList("lore"),
                d.getInt("amount", 1),
                d.getBoolean("glow", false),
                customModelData);
    }

    private static void writeDisplay(ConfigurationSection c, String path, DisplaySpec d) {
        c.set(path + ".material", d.material());
        if (d.name() != null) {
            c.set(path + ".name", d.name());
        }
        if (!d.lore().isEmpty()) {
            c.set(path + ".lore", d.lore());
        }
        c.set(path + ".amount", d.amount());
        c.set(path + ".glow", d.glow());
        if (d.customModelData() != null) {
            c.set(path + ".custom-model-data", d.customModelData());
        }
    }
}
