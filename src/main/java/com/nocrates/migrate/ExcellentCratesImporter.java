package com.nocrates.migrate;

import com.nocrates.crate.Crate;
import com.nocrates.crate.CrateRegistry;
import com.nocrates.item.ItemSpec;
import com.nocrates.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

/**
 * Imports ExcellentCrates crates (plugins/ExcellentCrates/crates/*.yml). Best-effort:
 * name, reward names/weights/commands and material-based preview items map; serialized
 * item blobs and nightcore-specific options are noted and skipped.
 */
public final class ExcellentCratesImporter implements Importer {

    private final File pluginsDir;
    private final CrateRegistry crates;

    public ExcellentCratesImporter(File pluginsDir, CrateRegistry crates) {
        this.pluginsDir = pluginsDir;
        this.crates = crates;
    }

    @Override
    public String id() {
        return "excellentcrates";
    }

    private File cratesDir() {
        return new File(pluginsDir, "ExcellentCrates/crates");
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
        String id = "ec_" + file.getName().replace(".yml", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
        Crate crate = crates.exists(id) ? crates.get(id) : new Crate(id);
        String name = firstString(yml, "Name", "Settings.Name", "name");
        if (name != null) crate.displayName(name);

        ConfigurationSection rewards = firstSection(yml, "Rewards.List", "Rewards", "rewards.list", "rewards");
        if (rewards != null) {
            for (String rewardId : rewards.getKeys(false)) {
                ConfigurationSection rs = rewards.getConfigurationSection(rewardId);
                if (rs == null) continue;
                Reward reward = new Reward(rewardId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", ""));
                double weight = rs.getDouble("Weight", rs.getDouble("Chance", rs.getDouble("weight", 10)));
                reward.percentage(weight);
                String rewardName = firstString(rs, "Name", "name");
                ItemSpec display = new ItemSpec(firstString(rs, "Preview.Material", "PreviewItem.Material",
                        "preview.material") != null
                        ? firstString(rs, "Preview.Material", "PreviewItem.Material", "preview.material")
                        : "CHEST");
                if (rewardName != null) display.name(rewardName);
                reward.displayItem(display);
                reward.winCommands().addAll(rs.getStringList("Commands"));
                reward.winCommands().addAll(rs.getStringList("commands"));
                if (rs.contains("Items") || rs.contains("items")) {
                    report.note(id + "/" + rewardId + ": serialized items skipped — re-add win-items in the editor");
                }
                crate.addReward(reward);
                report.rewardImported();
            }
        }
        crates.put(crate);
        report.crateImported();
    }

    private static String firstString(ConfigurationSection sec, String... paths) {
        for (String path : paths) {
            String value = sec.getString(path);
            if (value != null) return value;
        }
        return null;
    }

    private static ConfigurationSection firstSection(ConfigurationSection sec, String... paths) {
        for (String path : paths) {
            ConfigurationSection value = sec.getConfigurationSection(path);
            if (value != null) return value;
        }
        return null;
    }
}
