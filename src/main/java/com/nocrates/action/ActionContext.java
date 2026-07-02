package com.nocrates.action;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/** Everything an action needs at run time. */
public record ActionContext(Plugin plugin, Player player, Map<String, String> placeholders) {

    /** Replaces %key% tokens from the context placeholder map. */
    public String apply(String input) {
        if (input == null || input.indexOf('%') < 0 || placeholders.isEmpty()) return input;
        String out = input;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("%" + e.getKey() + "%", e.getValue());
        }
        return out;
    }
}
