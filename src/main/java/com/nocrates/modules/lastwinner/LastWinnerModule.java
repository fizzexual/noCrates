package com.nocrates.modules.lastwinner;

import com.nocrates.api.Addon;
import com.nocrates.api.events.RewardWinEvent;
import com.nocrates.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Last Winner module: rolling winner history per crate exposed
 * as placeholders — %nocrates_lastwinner_&lt;field&gt;_&lt;crate&gt;_&lt;n&gt;% with
 * field in player/reward/second/minute/hour/day/month/year and n = 1 (newest) .. keep.
 */
public final class LastWinnerModule extends Addon implements Listener {

    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();
    private int keep = 10;

    @Override
    public void onEnable() {
        keep = Math.max(1, config().getInt("keep", 10));
        Bukkit.getPluginManager().registerEvents(this, api().plugin());
        for (var crate : api().crates().all()) {
            api().storage().lastWinners(crate.id(), keep).thenAccept(rows ->
                    cache.put(crate.id(), new CopyOnWriteArrayList<>(rows)));
        }
        api().registerPlaceholder("lastwinner", (player, rest) -> resolve(rest));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWin(RewardWinEvent event) {
        String row = event.player().getName() + ";"
                + Text.plain(Text.mm(event.reward().displayName())) + ";"
                + Instant.now().getEpochSecond();
        api().storage().pushWinner(event.crate().id(), row, keep);
        List<String> rows = cache.computeIfAbsent(event.crate().id(), k -> new CopyOnWriteArrayList<>());
        rows.add(0, row);
        while (rows.size() > keep) rows.remove(rows.size() - 1);
    }

    /** rest = "<field>_<crate>_<n>" — crate ids may contain underscores, n is last. */
    private String resolve(String rest) {
        int firstUnderscore = rest.indexOf('_');
        int lastUnderscore = rest.lastIndexOf('_');
        if (firstUnderscore < 0 || lastUnderscore <= firstUnderscore) return "";
        String field = rest.substring(0, firstUnderscore);
        String crate = rest.substring(firstUnderscore + 1, lastUnderscore);
        int position;
        try {
            position = Integer.parseInt(rest.substring(lastUnderscore + 1));
        } catch (NumberFormatException e) {
            return "";
        }
        List<String> rows = cache.get(crate);
        if (rows == null || position < 1 || position > rows.size()) return "";
        String[] parts = rows.get(position - 1).split(";", 3);
        if (parts.length < 3) return "";
        switch (field) {
            case "player":
                return parts[0];
            case "reward":
                return parts[1];
            default:
                long epoch;
                try {
                    epoch = Long.parseLong(parts[2]);
                } catch (NumberFormatException e) {
                    return "";
                }
                ZonedDateTime time = Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault());
                return switch (field) {
                    case "second" -> String.format("%02d", time.getSecond());
                    case "minute" -> String.format("%02d", time.getMinute());
                    case "hour" -> String.format("%02d", time.getHour());
                    case "day" -> String.format("%02d", time.getDayOfMonth());
                    case "month" -> String.format("%02d", time.getMonthValue());
                    case "year" -> String.valueOf(time.getYear());
                    default -> "";
                };
        }
    }
}
