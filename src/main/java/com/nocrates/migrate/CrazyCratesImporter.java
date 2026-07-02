package com.nocrates.migrate;

import com.nocrates.crate.Crate;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.item.ItemSpec;
import com.nocrates.key.KeyLink;
import com.nocrates.key.KeyRegistry;
import com.nocrates.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Imports CrazyCrates crates (plugins/CrazyCrates/crates/*.yml). Best-effort: display
 * items, chances, commands and simple "Item:X, Amount:N" prize items map cleanly;
 * exotic prize options are noted and skipped.
 */
public final class CrazyCratesImporter implements Importer {

    private final File pluginsDir;
    private final CrateRegistry crates;
    private final KeyRegistry keys;

    public CrazyCratesImporter(File pluginsDir, CrateRegistry crates, KeyRegistry keys) {
        this.pluginsDir = pluginsDir;
        this.crates = crates;
        this.keys = keys;
    }

    @Override
    public String id() {
        return "crazycrates";
    }

    private File cratesDir() {
        return new File(pluginsDir, "CrazyCrates/crates");
    }

    @Override
    public boolean detect() {
        File[] files = cratesDir().listFiles((d, n) -> n.endsWith(".yml"));
        return files != null && files.length > 0;
    }

    @Override
    public ImportReport run() {
        ImportReport report = new ImportReport();
        File[] files = cratesDir().listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return report;
        for (File file : files) {
            try {
                importFile(file, report);
            } catch (Exception e) {
                report.note(file.getName() + ": " + e.getMessage());
            }
        }
        return report;
    }

    private void importFile(File file, ImportReport report) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("Crate");
        if (root == null) {
            report.note(file.getName() + ": no Crate section");
            return;
        }
        String id = "cc_" + file.getName().replace(".yml", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
        Crate crate = crates.exists(id) ? crates.get(id) : new Crate(id);
        crate.displayName(root.getString("CrateName", id));

        // key
        ConfigurationSection keySec = root.getConfigurationSection("PhysicalKey");
        if (keySec == null) keySec = root.getConfigurationSection("Key");
        if (keySec != null) {
            var key = keys.getOrCreate(id + "_key");
            ItemSpec item = new ItemSpec(keySec.getString("Item", "TRIPWIRE_HOOK"));
            item.name(keySec.getString("Name", "&aKey"));
            item.glow(keySec.getBoolean("Glowing", false));
            key.item(item);
            keys.save();
            crate.keys(List.of(new KeyLink(key.id(), 1, 0)));
        }

        ConfigurationSection prizes = root.getConfigurationSection("Prizes");
        if (prizes != null) {
            for (String prizeId : prizes.getKeys(false)) {
                ConfigurationSection prize = prizes.getConfigurationSection(prizeId);
                if (prize == null) continue;
                Reward reward = new Reward("prize-" + prizeId.toLowerCase(Locale.ROOT));
                reward.percentage(prize.getDouble("Chance", 10));
                ItemSpec display = new ItemSpec(prize.getString("DisplayItem", "CHEST"));
                display.amount(prize.getInt("DisplayAmount", 1));
                display.name(prize.getString("DisplayName"));
                if (!prize.getStringList("DisplayLore").isEmpty()) {
                    display.lore(prize.getStringList("DisplayLore"));
                }
                reward.displayItem(display);
                reward.winCommands().addAll(prize.getStringList("Commands"));
                for (String itemLine : prize.getStringList("Items")) {
                    ItemSpec parsed = parseItemLine(itemLine);
                    if (parsed != null) reward.winItems().add(parsed);
                    else report.note(id + "/" + prizeId + ": unparsed item '" + itemLine + "'");
                }
                if (prize.getBoolean("Firework", false)) {
                    report.note(id + "/" + prizeId + ": Firework flag not mapped");
                }
                crate.addReward(reward);
                report.rewardImported();
            }
        }
        crates.put(crate);
        report.crateImported();
    }

    /** CrazyCrates item strings: "Item:DIAMOND, Amount:5, Name:&bShiny". */
    static ItemSpec parseItemLine(String line) {
        if (line == null || line.isBlank()) return null;
        ItemSpec spec = null;
        for (String part : line.split(",")) {
            String[] kv = part.trim().split(":", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim().toLowerCase(Locale.ROOT);
            String v = kv[1].trim();
            switch (k) {
                case "item" -> spec = new ItemSpec(v.toUpperCase(Locale.ROOT));
                case "amount" -> {
                    if (spec != null) {
                        try {
                            spec.amount(Integer.parseInt(v));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                case "name" -> {
                    if (spec != null) spec.name(v);
                }
                default -> {
                }
            }
        }
        return spec;
    }
}
