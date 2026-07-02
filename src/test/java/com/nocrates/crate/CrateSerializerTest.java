package com.nocrates.crate;

import com.nocrates.reward.GuaranteedWin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateSerializerTest {

    private Crate loadExample() {
        var in = getClass().getClassLoader().getResourceAsStream("crates/example.yml");
        assertNotNull(in, "example.yml resource missing");
        var yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        return CrateSerializer.read("example", yml);
    }

    @Test
    void exampleParsesFully() {
        Crate crate = loadExample();
        assertEquals("example", crate.id());
        assertTrue(crate.enabled());
        assertEquals(Crate.EngineType.BLOCK, crate.engine());
        assertEquals(1, crate.keys().size());
        assertEquals("vote", crate.keys().get(0).keyId());
        assertEquals(4, crate.rewards().size());
        assertEquals(2, crate.animation().idleEffects().size());
        assertTrue(crate.guaranteedEnabled());
        assertEquals(GuaranteedWin.Mode.REPETITIVE, crate.guaranteedMode());
        assertEquals(1, crate.milestones().size());
        assertEquals(3, crate.hologramLines().size());

        var vip = crate.reward("vip_rank");
        assertNotNull(vip);
        assertEquals(1, vip.playerLimit().max());
        assertTrue(vip.broadcast());
        assertTrue(vip.alternative().enabled());
        assertEquals(1, vip.alternative().commands().size());
        assertEquals(1, vip.restrictedPermissions().size());
        // normalized chance of 50-weight reward across 100 total weight
        assertEquals(50.0, crate.normalizedChance(crate.reward("diamonds")), 0.001);
    }

    @Test
    void roundTripPreservesModel() {
        Crate crate = loadExample();
        YamlConfiguration out = new YamlConfiguration();
        CrateSerializer.write(crate, out);
        Crate reread = CrateSerializer.read("example", out);

        assertEquals(crate.displayName(), reread.displayName());
        assertEquals(crate.engine(), reread.engine());
        assertEquals(crate.blockMaterial(), reread.blockMaterial());
        assertEquals(crate.keys(), reread.keys());
        assertEquals(crate.rewards().keySet(), reread.rewards().keySet());
        assertEquals(crate.animation().idleEffects(), reread.animation().idleEffects());
        assertEquals(crate.animation().preOpen(), reread.animation().preOpen());
        assertEquals(crate.milestones(), reread.milestones());
        assertEquals(crate.rewardsMode(), reread.rewardsMode());
        assertEquals(crate.maxWinRewards(), reread.maxWinRewards());
        assertEquals(crate.hologramLines(), reread.hologramLines());
        assertEquals(crate.open().cooldownSeconds(), reread.open().cooldownSeconds());

        for (String id : crate.rewards().keySet()) {
            var a = crate.reward(id);
            var b = reread.reward(id);
            assertEquals(a.percentage(), b.percentage(), 0.0001, id);
            assertEquals(a.displayItem(), b.displayItem(), id);
            assertEquals(a.winItems(), b.winItems(), id);
            assertEquals(a.winCommands(), b.winCommands(), id);
            assertEquals(a.broadcast(), b.broadcast(), id);
            assertEquals(a.virtualReward(), b.virtualReward(), id);
            assertEquals(a.playerLimit(), b.playerLimit(), id);
            assertEquals(a.globalLimit(), b.globalLimit(), id);
            assertEquals(a.restrictedPermissions(), b.restrictedPermissions(), id);
            assertEquals(a.alternative().enabled(), b.alternative().enabled(), id);
            assertEquals(a.alternative().commands(), b.alternative().commands(), id);
            assertEquals(Objects.toString(a.rarity(), ""), Objects.toString(b.rarity(), ""), id);
        }
    }
}
