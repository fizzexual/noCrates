package com.nocrates.crate;

import com.nocrates.key.KeyType;
import com.nocrates.reward.DisplaySpec;
import com.nocrates.reward.Pity;
import com.nocrates.reward.Reward;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateSerializerTest {

    private Crate sample() {
        return Crate.builder("vote")
                .displayName("<gold>Vote Crate")
                .animation("csgo")
                .key(new CrateKeySpec(KeyType.BOTH, "vote", DisplaySpec.of("TRIPWIRE_HOOK", "<yellow>Vote Key")))
                .block(new CrateBlock(true, List.of("world,100,65,200"),
                        List.of("<bold>VOTE CRATE", "<gray>Right-click to open"), "VILLAGER_HAPPY"))
                .preview(true, "Vote — Rewards")
                .pity(new Pity(true, 25, "legendary"))
                .broadcast(true)
                .cooldownSeconds(5)
                .openSound("block.note_block.pling")
                .addReward(new Reward("diamonds", "common", 50.0,
                        new DisplaySpec("DIAMOND", "<aqua>5 Diamonds", List.of("<gray>Shiny!"), 5, false, null),
                        List.of("item: DIAMOND 5", "message: <green>Nice"), -1, 0))
                .addReward(new Reward("vip", "legendary", 2.0,
                        DisplaySpec.of("NETHER_STAR", "<gold>VIP Rank"),
                        List.of("command: lp user %player% parent add vip", "broadcast: <gold>%player% won VIP!"), 1, 0))
                .build();
    }

    @Test
    void crateRoundTripsThroughYaml() throws Exception {
        Crate original = sample();

        YamlConfiguration out = new YamlConfiguration();
        CrateSerializer.write(original, out);
        String text = out.saveToString();

        YamlConfiguration in = new YamlConfiguration();
        in.loadFromString(text);
        Crate back = CrateSerializer.read("vote", in);

        assertEquals(original.displayName(), back.displayName());
        assertEquals(original.animation(), back.animation());
        assertEquals(original.key().type(), back.key().type());
        assertEquals(original.key().keyId(), back.key().keyId());
        assertNotNull(back.key().item());
        assertEquals("TRIPWIRE_HOOK", back.key().item().material());

        assertTrue(back.pity().enabled());
        assertEquals(25, back.pity().every());
        assertEquals("legendary", back.pity().tier());

        assertEquals(original.broadcast(), back.broadcast());
        assertEquals(original.cooldownSeconds(), back.cooldownSeconds());
        assertEquals(original.openSound(), back.openSound());

        assertNotNull(back.block());
        assertEquals(List.of("world,100,65,200"), back.block().locations());
        assertEquals(2, back.block().hologram().size());

        assertEquals(2, back.rewards().size());
        Reward diamonds = back.rewards().stream().filter(r -> r.id().equals("diamonds")).findFirst().orElseThrow();
        assertEquals(50.0, diamonds.weight());
        assertEquals("common", diamonds.rarityId());
        assertEquals(5, diamonds.display().amount());
        assertEquals(List.of("item: DIAMOND 5", "message: <green>Nice"), diamonds.actions());

        Reward vip = back.rewards().stream().filter(r -> r.id().equals("vip")).findFirst().orElseThrow();
        assertEquals(1, vip.maxPerPlayer());
        assertEquals("legendary", vip.rarityId());
    }
}
