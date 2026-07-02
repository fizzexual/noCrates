package com.nocrates.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text pipeline: accepts MiniMessage, legacy ampersand/section codes and hex
 * ({@code &#RRGGBB} or bungee {@code &x&R&R&G&G&B&B}) in any mix, converts the legacy
 * parts to MiniMessage tags, then deserializes. Parse failures degrade to plain text
 * instead of throwing.
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX_AMP = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern HEX_BUNGEE = Pattern.compile("(?i)[&§]x([&§][0-9a-f]){6}");
    private static final String[] CODE_TAGS = new String[128];

    static {
        CODE_TAGS['0'] = "<black>";
        CODE_TAGS['1'] = "<dark_blue>";
        CODE_TAGS['2'] = "<dark_green>";
        CODE_TAGS['3'] = "<dark_aqua>";
        CODE_TAGS['4'] = "<dark_red>";
        CODE_TAGS['5'] = "<dark_purple>";
        CODE_TAGS['6'] = "<gold>";
        CODE_TAGS['7'] = "<gray>";
        CODE_TAGS['8'] = "<dark_gray>";
        CODE_TAGS['9'] = "<blue>";
        CODE_TAGS['a'] = "<green>";
        CODE_TAGS['b'] = "<aqua>";
        CODE_TAGS['c'] = "<red>";
        CODE_TAGS['d'] = "<light_purple>";
        CODE_TAGS['e'] = "<yellow>";
        CODE_TAGS['f'] = "<white>";
        CODE_TAGS['k'] = "<obfuscated>";
        CODE_TAGS['l'] = "<bold>";
        CODE_TAGS['m'] = "<strikethrough>";
        CODE_TAGS['n'] = "<underlined>";
        CODE_TAGS['o'] = "<italic>";
        CODE_TAGS['r'] = "<reset>";
    }

    private Text() {
    }

    public static Component mm(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        String converted = legacyToMini(input);
        try {
            return MM.deserialize(converted);
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    public static List<Component> mmList(List<String> in) {
        List<Component> out = new ArrayList<>();
        if (in != null) for (String s : in) out.add(mm(s));
        return out;
    }

    public static String plain(Component c) {
        return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** Converts legacy color codes to MiniMessage tags; leaves existing tags untouched. */
    public static String legacyToMini(String input) {
        String s = input;
        Matcher bungee = HEX_BUNGEE.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (bungee.find()) {
            String hex = bungee.group().replaceAll("(?i)[&§x]", "");
            bungee.appendReplacement(sb, "<#" + hex + ">");
        }
        bungee.appendTail(sb);
        s = sb.toString();
        s = HEX_AMP.matcher(s).replaceAll("<#$1>");
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) {
                char code = Character.toLowerCase(s.charAt(i + 1));
                if (code < 128 && CODE_TAGS[code] != null) {
                    out.append(CODE_TAGS[code]);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
