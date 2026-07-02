package com.nocrates.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {

    @Test
    void legacyCodesBecomeTags() {
        assertEquals("<red>Hi <bold>there", Text.legacyToMini("&cHi &lthere"));
        assertEquals("<#A1B2C3>hex", Text.legacyToMini("&#A1B2C3hex"));
        assertEquals("<#aabbcc>bungee", Text.legacyToMini("&x&a&a&b&b&c&cbungee"));
        assertEquals("<yellow>mixed <green>tags</green>", Text.legacyToMini("&emixed <green>tags</green>"));
    }

    @Test
    void plainSerialization() {
        assertEquals("Hello world", Text.plain(Text.mm("<red>Hello <bold>world")));
    }
}
