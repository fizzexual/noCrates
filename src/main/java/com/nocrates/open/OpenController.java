package com.nocrates.open;

import com.nocrates.NoCrates;
import com.nocrates.animation.AnimationRegistry;
import com.nocrates.animation.CrateSession;
import com.nocrates.crate.Crate;
import com.nocrates.key.KeyManager;
import com.nocrates.message.Messages;
import com.nocrates.reward.Rarity;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RewardActions;
import com.nocrates.reward.RollEngine;
import com.nocrates.storage.PlayerData;
import com.nocrates.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates a crate opening: validate a key, consume it, roll the outcome
 * (honouring pity and per-player limits), play the animation, then grant rewards
 * and record stats when the animation finishes.
 */
public final class OpenController {

    private final NoCrates plugin;
    private final AnimationRegistry animations;
    private final RollEngine roll = new RollEngine(new Random());
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Set<UUID> opening = new HashSet<>();

    public OpenController(NoCrates plugin, AnimationRegistry animations) {
        this.plugin = plugin;
        this.animations = animations;
    }

    public void open(Player player, Crate crate) {
        Messages messages = plugin.services().messages();
        UUID id = player.getUniqueId();

        if (opening.contains(id)) {
            messages.send(player, "already-opening");
            return;
        }
        if (crate.rewards().isEmpty()) {
            messages.send(player, "crate-empty");
            return;
        }

        long now = System.currentTimeMillis();
        String cooldownKey = id + ":" + crate.name();
        Long until = cooldowns.get(cooldownKey);
        if (until != null && now < until) {
            long seconds = (until - now + 999) / 1000;
            messages.send(player, "on-cooldown", Messages.ph("time", String.valueOf(seconds)));
            return;
        }

        KeyManager keys = plugin.services().keys();
        if (!keys.has(player, crate) || !keys.consumeOne(player, crate)) {
            messages.send(player, "no-key",
                    Messages.ph("crate", crate.name()),
                    Messages.ph("key", crate.key().keyId()));
            return;
        }

        PlayerData data = plugin.services().playerData().get(id);
        List<Reward> available = availableRewards(crate, data);
        Reward won = crate.pity().shouldForce(data.opens(crate.name()))
                ? roll.rollTier(available, crate.pity().tier())
                : roll.roll(available);
        if (won == null) {
            keys.giveVirtual(id, crate, 1); // refund the consumed key
            messages.send(player, "crate-empty");
            return;
        }

        if (crate.cooldownSeconds() > 0) {
            cooldowns.put(cooldownKey, now + crate.cooldownSeconds() * 1000L);
        }

        List<Reward> outcome = List.of(won);
        opening.add(id);
        CrateSession session = new CrateSession(player, crate, outcome, () -> grant(player, crate, outcome));
        animations.get(crate.animation()).play(session);
    }

    private List<Reward> availableRewards(Crate crate, PlayerData data) {
        List<Reward> available = new ArrayList<>();
        for (Reward reward : crate.rewards()) {
            if (reward.maxPerPlayer() >= 0 && data.winCount(crate.name(), reward.id()) >= reward.maxPerPlayer()) {
                continue;
            }
            available.add(reward);
        }
        return available.isEmpty() ? crate.rewards() : available;
    }

    private void grant(Player player, Crate crate, List<Reward> outcome) {
        opening.remove(player.getUniqueId());
        PlayerData data = plugin.services().playerData().get(player.getUniqueId());
        Messages messages = plugin.services().messages();
        data.incrOpens(crate.name());

        for (Reward reward : outcome) {
            for (String line : reward.actions()) {
                RewardActions.parse(line).execute(player);
            }
            data.incrWin(crate.name(), reward.id());

            Rarity rarity = plugin.services().rarities().get(reward.rarityId());
            Component rewardName = rewardName(reward, rarity);
            messages.send(player, "reward-won", Messages.phComponent("reward", rewardName));
            if (crate.broadcast() && rarity.broadcast()) {
                messages.broadcast("broadcast-win",
                        Messages.ph("player", player.getName()),
                        Messages.ph("crate", crate.name()),
                        Messages.phComponent("reward", rewardName));
            }
        }
    }

    private Component rewardName(Reward reward, Rarity rarity) {
        String raw = (reward.display() != null && reward.display().name() != null && !reward.display().name().isBlank())
                ? reward.display().name()
                : rarity.wrap(reward.id());
        return Items.mini(raw);
    }
}
