package com.nocrates.migrate;

import com.nocrates.item.ItemSpec;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImporterTest {

    private YamlConfiguration fixture(String path) {
        var in = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(in, path + " fixture missing");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    @Test
    void crazyCratesItemLineParsing() {
        ItemSpec spec = CrazyCratesImporter.parseItemLine("Item:DIAMOND, Amount:5, Name:&bShiny");
        assertNotNull(spec);
        assertEquals("DIAMOND", spec.material());
        assertEquals(5, spec.amount());
        assertEquals("&bShiny", spec.name());
        assertNull(CrazyCratesImporter.parseItemLine("Amount:5"));
        assertNull(CrazyCratesImporter.parseItemLine(""));
    }

    @Test
    void crazyCratesFixtureHasExpectedShape() {
        var yml = fixture("migrate/CrazyCrates/crates/Basic.yml");
        var crate = yml.getConfigurationSection("Crate");
        assertNotNull(crate);
        assertEquals("&7Basic Crate", crate.getString("CrateName"));
        var prizes = crate.getConfigurationSection("Prizes");
        assertNotNull(prizes);
        assertEquals(3, prizes.getKeys(false).size());
        assertEquals(50.0, prizes.getDouble("1.Chance"));
        assertEquals("DIAMOND", prizes.getString("1.DisplayItem"));
        assertEquals(1, prizes.getStringList("1.Commands").size());
    }

    @Test
    void excellentCratesFixtureHasExpectedShape() {
        var yml = fixture("migrate/ExcellentCrates/crates/epic.yml");
        assertEquals("&6&lEpic Crate", yml.getString("Name"));
        var rewards = yml.getConfigurationSection("Rewards.List");
        assertNotNull(rewards);
        assertEquals(3, rewards.getKeys(false).size());
        assertEquals(60.0, rewards.getDouble("money.Weight"));
        assertEquals("SUNFLOWER", rewards.getString("money.Preview.Material"));
        assertEquals(1, rewards.getStringList("rank.Commands").size());
    }
}
