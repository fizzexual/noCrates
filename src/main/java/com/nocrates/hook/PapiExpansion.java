package com.nocrates.hook;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.reward.GuaranteedWin;
import com.nocrates.text.Times;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * %nocrates_...% placeholders. Modules/addons can register extra sub-placeholders
 * (e.g. last-winner) via {@link #register}.
 */
public final class PapiExpansion extends PlaceholderExpansion {

    private static final Map<String, BiFunction<OfflinePlayer, String, String>> EXTRA = new ConcurrentHashMap<>();

    public static void register(String prefix, BiFunction<OfflinePlayer, String, String> resolver) {
        EXTRA.put(prefix.toLowerCase(Locale.ROOT), resolver);
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
        return Services.get().plugin().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String p = params.toLowerCase(Locale.ROOT);
        var services = Services.get();

        for (Map.Entry<String, BiFunction<OfflinePlayer, String, String>> extra : EXTRA.entrySet()) {
            if (p.startsWith(extra.getKey() + "_")) {
                return extra.getValue().apply(player, p.substring(extra.getKey().length() + 1));
            }
        }
        if (p.equals("opened_total")) {
            return player instanceof Player online
                    ? String.valueOf(services.players().of(online).totalOpens()) : "0";
        }
        if (p.startsWith("keys_")) {
            String keyId = p.substring(5);
            if (!(player instanceof Player online)) return "0";
            int virtual = services.players().of(online).keys(keyId);
            int physical = services.keyService().countPhysical(online, keyId);
            return String.valueOf(virtual + physical);
        }
        if (p.startsWith("cooldown_")) {
            String crateId = p.substring(9);
            if (!(player instanceof Player online)) return "";
            long until = services.players().of(online).cooldownUntil(crateId);
            long left = until - Instant.now().getEpochSecond();
            return left <= 0 ? services.lang().rawString("placeholder-ready")
                    : Times.format(services.lang(), left);
        }
        if (p.startsWith("opened_")) {
            String crateId = p.substring(7);
            return player instanceof Player online
                    ? String.valueOf(services.players().of(online).opens(crateId)) : "0";
        }
        if (p.startsWith("rerolls_")) {
            String crateId = p.substring(8);
            return player instanceof Player online
                    ? String.valueOf(services.players().of(online).rerolls(crateId)) : "0";
        }
        if (p.startsWith("guaranteed_amount_")) {
            return guaranteedAmount(player, p.substring(18));
        }
        if (p.startsWith("guaranteed_reward_")) {
            return guaranteedReward(player, p.substring(18));
        }
        if (p.startsWith("winlimit_")) {
            String[] parts = p.substring(9).split("_", 2);
            if (parts.length != 2 || !(player instanceof Player online)) return "0";
            return String.valueOf(services.players().of(online).wins(parts[0], parts[1]));
        }
        if (p.startsWith("globalwinlimit_")) {
            String[] parts = p.substring(15).split("_", 2);
            if (parts.length != 2) return "0";
            return String.valueOf(services.winLimits().globalCount(parts[0], parts[1]));
        }
        return null;
    }

    private String guaranteedAmount(OfflinePlayer player, String crateId) {
        var services = Services.get();
        Crate crate = services.crates().get(crateId);
        if (crate == null || !crate.guaranteedEnabled() || crate.milestones().isEmpty()
                || !(player instanceof Player online)) {
            return "";
        }
        var data = services.players().of(online);
        int opens = data.opens(crate.id());
        if (crate.guaranteedMode() == GuaranteedWin.Mode.SEQUENTIAL) {
            int index = data.milestoneIndex(crate.id());
            if (index >= crate.milestones().size()) return "";
            return String.valueOf(Math.max(0, crate.milestones().get(index).openings() - opens));
        }
        int best = Integer.MAX_VALUE;
        for (var milestone : crate.milestones()) {
            if (milestone.openings() <= 0) continue;
            int until = milestone.openings() - (opens % milestone.openings());
            best = Math.min(best, until);
        }
        return best == Integer.MAX_VALUE ? "" : String.valueOf(best);
    }

    private String guaranteedReward(OfflinePlayer player, String crateId) {
        var services = Services.get();
        Crate crate = services.crates().get(crateId);
        if (crate == null || crate.milestones().isEmpty()) return "";
        if (crate.guaranteedMode() == GuaranteedWin.Mode.SEQUENTIAL && player instanceof Player online) {
            int index = services.players().of(online).milestoneIndex(crate.id());
            if (index >= crate.milestones().size()) return "";
            var reward = crate.reward(crate.milestones().get(index).rewardId());
            return reward == null ? "" : reward.displayName();
        }
        var reward = crate.reward(crate.milestones().get(0).rewardId());
        return reward == null ? "" : reward.displayName();
    }
}
