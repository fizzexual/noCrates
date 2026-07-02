package com.nocrates.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatSoundTest {

    @Test
    void enumNamesResolveToRealRegistryKeys() {
        // multi-word segments keep their underscores
        assertEquals("minecraft:block.note_block.chime", Compat.soundKey("BLOCK_NOTE_BLOCK_CHIME"));
        assertEquals("minecraft:entity.experience_orb.pickup", Compat.soundKey("ENTITY_EXPERIENCE_ORB_PICKUP"));
        assertEquals("minecraft:entity.warden.sonic_boom", Compat.soundKey("ENTITY_WARDEN_SONIC_BOOM"));
        assertEquals("minecraft:block.enchantment_table.use", Compat.soundKey("BLOCK_ENCHANTMENT_TABLE_USE"));
        assertEquals("minecraft:ui.button.click", Compat.soundKey("UI_BUTTON_CLICK"));
    }

    @Test
    void keyStyleNamesPassThrough() {
        assertEquals("minecraft:block.note_block.chime", Compat.soundKey("minecraft:block.note_block.chime"));
        assertEquals("block.note_block.chime", Compat.soundKey("BLOCK.NOTE_BLOCK.CHIME"));
        assertEquals("custom:my.sound", Compat.soundKey("custom:my.sound"));
    }
}
