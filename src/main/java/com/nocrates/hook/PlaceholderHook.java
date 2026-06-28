package com.nocrates.hook;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion exposing per-player crate stats:
 * {@code %nocrates_keys_<crate>%}, {@code %nocrates_opens_<crate>%},
 * {@code %nocrates_pity_<crate>%} (opens remaining until the next milestone).
 *
 * <p>Only loaded/registered when PlaceholderAPI is installed.
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final NoCrates plugin;

    public PlaceholderHook(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nocrates";
    }

    @Override
    public String getAuthor() {
        return "fizzexual";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        int idx = params.indexOf('_');
        if (idx < 0) {
            return null;
        }
        String type = params.substring(0, idx).toLowerCase();
        Crate crate = plugin.services().crates().get(params.substring(idx + 1));
        if (crate == null) {
            return "";
        }
        return switch (type) {
            case "keys" -> {
                Player online = player.getPlayer();
                yield online != null
                        ? String.valueOf(plugin.services().keys().total(online, crate))
                        : String.valueOf(plugin.services().playerData().get(player.getUniqueId())
                                .keys(crate.name(), crate.key().keyId()));
            }
            case "opens" -> String.valueOf(
                    plugin.services().playerData().get(player.getUniqueId()).opens(crate.name()));
            case "pity" -> {
                int every = crate.pity().every();
                if (every <= 0) {
                    yield "0";
                }
                int opens = plugin.services().playerData().get(player.getUniqueId()).opens(crate.name());
                yield String.valueOf(every - (opens % every));
            }
            default -> null;
        };
    }
}
