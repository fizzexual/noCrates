package com.nocrates.crate;

import com.nocrates.item.ItemSpec;
import com.nocrates.key.KeyLink;
import com.nocrates.reward.AlternativeReward;
import com.nocrates.reward.GuaranteedWin;
import com.nocrates.reward.Reward;
import com.nocrates.reward.WinLimit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Full YAML round-trip for crates; the schema is the file format documented in README. */
public final class CrateSerializer {

    private CrateSerializer() {
    }

    // ---------------- read ----------------

    public static Crate read(String id, ConfigurationSection c) {
        Crate crate = new Crate(id);
        crate.enabled(c.getBoolean("enabled", true));
        crate.displayName(c.getString("display-name", crate.displayName()));

        String engine = c.getString("engine.type", "BLOCK").toUpperCase(Locale.ROOT);
        crate.engine(engine.equals("MODEL") ? Crate.EngineType.MODEL : Crate.EngineType.BLOCK);
        crate.blockMaterial(c.getString("engine.block-material", "CHEST"));
        if (c.isConfigurationSection("engine.model-item")) {
            crate.modelItem(ItemSpec.fromConfig(c.getConfigurationSection("engine.model-item")));
        }
        crate.modelYOffset(c.getDouble("engine.model-y-offset", 0.0));
        crate.modelYaw((float) c.getDouble("engine.model-yaw", 0.0));

        crate.locations().addAll(c.getStringList("locations"));

        crate.permissionRequired(c.getBoolean("permission.required", false));
        crate.permission(c.getString("permission.node", crate.permission()));

        crate.open().cost(c.getDouble("open.cost", 0));
        crate.open().cooldownSeconds(c.getInt("open.cooldown-seconds", 0));
        crate.open().quickOpen(c.getBoolean("open.quick-open", true));
        crate.open().simultaneous(c.getBoolean("open.simultaneous", true));
        crate.open().knockback(c.getBoolean("open.knockback", true));

        List<KeyLink> links = new ArrayList<>();
        for (Map<?, ?> raw : c.getMapList("keys")) {
            Object keyId = raw.get("key");
            if (keyId == null) continue;
            int amount = raw.get("amount") instanceof Number n ? n.intValue() : 1;
            int priority = raw.get("priority") instanceof Number n ? n.intValue() : 0;
            links.add(new KeyLink(String.valueOf(keyId), amount, priority));
        }
        crate.keys(links);

        crate.hologramLines(c.getStringList("hologram.lines"));
        crate.hologramOffset(c.getDouble("hologram.offset", 1.6));

        crate.previewEnabled(c.getBoolean("preview.enabled", true));
        crate.previewMenu(c.getString("preview.menu", "preview"));

        crate.rewardsMode(c.getString("rewards-mode", "RANDOM").equalsIgnoreCase("SELECTIVE")
                ? RewardsMode.SELECTIVE : RewardsMode.RANDOM);
        crate.maxWinRewards(c.getInt("max-win-rewards", 1));

        ConfigurationSection anim = c.getConfigurationSection("animation");
        if (anim != null) {
            crate.animation().idleEffects(anim.getStringList("idle"));
            crate.animation().preOpen(anim.getString("pre-open", "DEFAULT"));
            crate.animation().postOpen(anim.getString("post-open", "BALL"));
            crate.animation().rewardDisplay(anim.getString("reward-display", "DEFAULT"));
            crate.animation().preDelayTicks(anim.getInt("pre-delay-ticks", 30));
            crate.animation().postDelayTicks(anim.getInt("post-delay-ticks", 30));
            crate.animation().displayDurationTicks(anim.getInt("display-duration-ticks", 60));
        }

        ConfigurationSection rewards = c.getConfigurationSection("rewards");
        if (rewards != null) {
            for (String rid : rewards.getKeys(false)) {
                ConfigurationSection rs = rewards.getConfigurationSection(rid);
                if (rs != null) crate.addReward(readReward(rid, rs));
            }
        }

        crate.guaranteedEnabled(c.getBoolean("guaranteed.enabled", false));
        crate.guaranteedMode(c.getString("guaranteed.mode", "REPETITIVE").equalsIgnoreCase("SEQUENTIAL")
                ? GuaranteedWin.Mode.SEQUENTIAL : GuaranteedWin.Mode.REPETITIVE);
        List<GuaranteedWin.Milestone> milestones = new ArrayList<>();
        for (Map<?, ?> raw : c.getMapList("guaranteed.milestones")) {
            int openings = raw.get("openings") instanceof Number n ? n.intValue() : 0;
            double chance = raw.get("chance") instanceof Number n ? n.doubleValue() : 0;
            Object reward = raw.get("reward");
            if (reward != null) milestones.add(new GuaranteedWin.Milestone(openings, String.valueOf(reward), chance));
        }
        crate.milestones(milestones);

        crate.rerollEnabled(c.getBoolean("reroll.enabled", false));
        crate.rerollFree(c.getInt("reroll.free", 0));
        Map<String, Integer> groups = new LinkedHashMap<>();
        ConfigurationSection groupSec = c.getConfigurationSection("reroll.groups");
        if (groupSec != null) {
            for (String g : groupSec.getKeys(false)) groups.put(g, groupSec.getInt(g, 1));
        }
        crate.rerollGroups(groups);

        crate.broadcastMessage(c.getString("broadcast"));
        return crate;
    }

    private static Reward readReward(String id, ConfigurationSection rs) {
        Reward reward = new Reward(id.toLowerCase(Locale.ROOT));
        reward.percentage(rs.getDouble("percentage", 10));
        if (rs.isConfigurationSection("display-item")) {
            reward.displayItem(ItemSpec.fromConfig(rs.getConfigurationSection("display-item")));
        }
        ConfigurationSection winItems = rs.getConfigurationSection("win-items");
        if (winItems != null) {
            for (String k : winItems.getKeys(false)) {
                reward.winItems().add(ItemSpec.fromConfig(winItems.getConfigurationSection(k)));
            }
        }
        reward.winCommands().addAll(rs.getStringList("win-commands"));
        reward.broadcast(rs.getBoolean("broadcast", false));
        reward.virtualReward(rs.getBoolean("virtual", false));
        reward.shareOnline(rs.getBoolean("share-online", false));
        reward.restrictedPermissions().addAll(rs.getStringList("restricted-permissions"));
        reward.playerLimit(new WinLimit(rs.getInt("win-limits.player", -1), rs.getLong("win-limits.player-cooldown", 0)));
        reward.globalLimit(new WinLimit(rs.getInt("win-limits.global", -1), rs.getLong("win-limits.global-cooldown", 0)));
        reward.rarity(rs.getString("rarity"));
        reward.selectiveCost(rs.getInt("selective-cost", 1));
        reward.always(rs.getBoolean("always", false));
        ConfigurationSection alt = rs.getConfigurationSection("alternative-reward");
        if (alt != null) {
            AlternativeReward alternative = new AlternativeReward();
            alternative.enabled(alt.getBoolean("enabled", false));
            if (alt.isConfigurationSection("item")) {
                alternative.item(ItemSpec.fromConfig(alt.getConfigurationSection("item")));
            }
            alternative.displayName(alt.getString("display-name", ""));
            alternative.virtualOnly(alt.getBoolean("virtual", false));
            alternative.broadcast(alt.getBoolean("broadcast", false));
            alternative.commands(alt.getStringList("commands"));
            reward.alternative(alternative);
        }
        return reward;
    }

    // ---------------- write ----------------

    public static void write(Crate crate, ConfigurationSection c) {
        c.set("enabled", crate.enabled());
        c.set("display-name", crate.displayName());
        c.set("engine.type", crate.engine().name());
        c.set("engine.block-material", crate.blockMaterial());
        crate.modelItem().write(section(c, "engine.model-item"));
        if (crate.modelYOffset() != 0) c.set("engine.model-y-offset", crate.modelYOffset());
        if (crate.modelYaw() != 0) c.set("engine.model-yaw", (double) crate.modelYaw());
        c.set("locations", new ArrayList<>(crate.locations()));
        c.set("permission.required", crate.permissionRequired());
        c.set("permission.node", crate.permission());
        c.set("open.cost", crate.open().cost());
        c.set("open.cooldown-seconds", crate.open().cooldownSeconds());
        c.set("open.quick-open", crate.open().quickOpen());
        c.set("open.simultaneous", crate.open().simultaneous());
        c.set("open.knockback", crate.open().knockback());

        List<Map<String, Object>> links = new ArrayList<>();
        for (KeyLink link : crate.keys()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", link.keyId());
            m.put("amount", link.amount());
            m.put("priority", link.priority());
            links.add(m);
        }
        c.set("keys", links);

        c.set("hologram.lines", crate.hologramLines());
        c.set("hologram.offset", crate.hologramOffset());
        c.set("preview.enabled", crate.previewEnabled());
        c.set("preview.menu", crate.previewMenu());
        c.set("rewards-mode", crate.rewardsMode().name());
        c.set("max-win-rewards", crate.maxWinRewards());

        c.set("animation.idle", crate.animation().idleEffects());
        c.set("animation.pre-open", crate.animation().preOpen());
        c.set("animation.post-open", crate.animation().postOpen());
        c.set("animation.reward-display", crate.animation().rewardDisplay());
        c.set("animation.pre-delay-ticks", crate.animation().preDelayTicks());
        c.set("animation.post-delay-ticks", crate.animation().postDelayTicks());
        c.set("animation.display-duration-ticks", crate.animation().displayDurationTicks());

        for (Reward reward : crate.rewards().values()) {
            writeReward(reward, section(c, "rewards." + reward.id()));
        }

        c.set("guaranteed.enabled", crate.guaranteedEnabled());
        c.set("guaranteed.mode", crate.guaranteedMode().name());
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (GuaranteedWin.Milestone m : crate.milestones()) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("openings", m.openings());
            raw.put("reward", m.rewardId());
            if (m.chance() > 0) raw.put("chance", m.chance());
            milestones.add(raw);
        }
        c.set("guaranteed.milestones", milestones);

        c.set("reroll.enabled", crate.rerollEnabled());
        c.set("reroll.free", crate.rerollFree());
        for (Map.Entry<String, Integer> g : crate.rerollGroups().entrySet()) {
            c.set("reroll.groups." + g.getKey(), g.getValue());
        }
        c.set("broadcast", crate.broadcastMessage());
    }

    private static void writeReward(Reward reward, ConfigurationSection rs) {
        rs.set("percentage", reward.percentage());
        reward.displayItem().write(section(rs, "display-item"));
        int i = 0;
        for (ItemSpec item : reward.winItems()) {
            item.write(section(rs, "win-items." + i++));
        }
        if (!reward.winCommands().isEmpty()) rs.set("win-commands", reward.winCommands());
        if (reward.broadcast()) rs.set("broadcast", true);
        if (reward.virtualReward()) rs.set("virtual", true);
        if (reward.shareOnline()) rs.set("share-online", true);
        if (!reward.restrictedPermissions().isEmpty()) rs.set("restricted-permissions", reward.restrictedPermissions());
        if (!reward.playerLimit().unlimited() || reward.playerLimit().cooldownSeconds() > 0) {
            rs.set("win-limits.player", reward.playerLimit().max());
            rs.set("win-limits.player-cooldown", reward.playerLimit().cooldownSeconds());
        }
        if (!reward.globalLimit().unlimited() || reward.globalLimit().cooldownSeconds() > 0) {
            rs.set("win-limits.global", reward.globalLimit().max());
            rs.set("win-limits.global-cooldown", reward.globalLimit().cooldownSeconds());
        }
        if (reward.rarity() != null) rs.set("rarity", reward.rarity());
        if (reward.selectiveCost() != 1) rs.set("selective-cost", reward.selectiveCost());
        if (reward.always()) rs.set("always", true);
        if (reward.alternative().enabled()) {
            ConfigurationSection alt = section(rs, "alternative-reward");
            alt.set("enabled", true);
            if (reward.alternative().item() != null) {
                reward.alternative().item().write(section(alt, "item"));
            }
            if (!reward.alternative().displayName().isEmpty()) alt.set("display-name", reward.alternative().displayName());
            if (reward.alternative().virtualOnly()) alt.set("virtual", true);
            if (reward.alternative().broadcast()) alt.set("broadcast", true);
            if (!reward.alternative().commands().isEmpty()) alt.set("commands", reward.alternative().commands());
        }
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        ConfigurationSection s = parent.getConfigurationSection(path);
        return s != null ? s : parent.createSection(path);
    }
}
