package com.nocrates.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemSpecTest {

    @Test
    void yamlRoundTripKeepsEveryField() {
        YamlConfiguration in = new YamlConfiguration();
        var s = in.createSection("item");
        s.set("material", "TRIPWIRE_HOOK");
        s.set("amount", 3);
        s.set("name", "<yellow>Vote Key");
        s.set("lore", List.of("<gray>Line 1", "<gray>Line 2"));
        s.set("glow", true);
        s.set("custom-model-data", 1234);
        s.set("item-model", "nocrates:vote_key");
        s.set("head-texture", "eyJ0ZXh0dXJlcyI6e319");
        s.set("unbreakable", true);
        s.set("custom-item", "itemsadder:ruby");
        s.set("enchants.SHARPNESS", 2);
        s.set("flags", List.of("HIDE_ATTRIBUTES"));

        ItemSpec spec = ItemSpec.fromConfig(s);
        YamlConfiguration out = new YamlConfiguration();
        spec.write(out.createSection("item"));
        ItemSpec reread = ItemSpec.fromConfig(out.getConfigurationSection("item"));

        assertEquals(spec, reread);
        assertEquals("TRIPWIRE_HOOK", reread.material());
        assertEquals(3, reread.amount());
        assertEquals("<yellow>Vote Key", reread.name());
        assertEquals(2, reread.lore().size());
        assertEquals(1234, reread.customModelData());
        assertEquals("itemsadder:ruby", reread.customItem());
        assertEquals(2, reread.enchants().get("SHARPNESS"));
    }

    @Test
    void minimalSpecOmitsDefaults() {
        ItemSpec spec = new ItemSpec("DIAMOND");
        YamlConfiguration out = new YamlConfiguration();
        var sec = out.createSection("item");
        spec.write(sec);
        assertEquals("DIAMOND", sec.getString("material"));
        assertNull(sec.get("amount"));
        assertNull(sec.get("glow"));
        assertNull(sec.get("custom-model-data"));
    }

    @Test
    void headTextureUrlExtraction() {
        // {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/abc"}}}
        String b64 = java.util.Base64.getEncoder().encodeToString(
                "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abc\"}}}".getBytes());
        assertEquals("http://textures.minecraft.net/texture/abc", Heads.extractUrl(b64));
        assertNull(Heads.extractUrl("not-base64!!"));
    }
}
