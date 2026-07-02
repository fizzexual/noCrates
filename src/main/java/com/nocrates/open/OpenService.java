package com.nocrates.open;

import com.nocrates.api.events.CrateOpenEvent;
import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.crate.RewardsMode;
import com.nocrates.hook.VaultHook;
import com.nocrates.reward.GuaranteedWin;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RollEngine;
import com.nocrates.storage.PlayerData;
import com.nocrates.text.Times;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The opening pipeline: checks -> cost confirmation -> roll -> event -> consume ->
 * animate -> grant. Rewards are rolled before anything is taken from the player, so
 * every abort path is free.
 */
public final class OpenService {

    private final RollEngine rollEngine = new RollEngine(new Random());
    private final Random rng = new Random();

    /** Entry point for crate clicks and /crates open. */
    public void attempt(Player player, Crate crate, CratePlacement placement, boolean quickRequested) {
        var services = Services.get();
        var lang = services.lang();

        if (!crate.enabled()) {
            lang.send(player, "open-crate-disabled");
            return;
        }
        if (crate.permissionRequired() && !player.hasPermission(crate.permission())) {
            lang.send(player, "open-no-crate-permission");
            return;
        }
        PlayerData data = services.players().of(player);
        long now = Instant.now().getEpochSecond();
        if (data.cooldownUntil(crate.id()) > now) {
            lang.send(player, "open-cooldown",
                    Placeholder.unparsed("time", Times.format(lang, data.cooldownUntil(crate.id()) - now)));
            return;
        }
        if (!crate.keys().isEmpty() && !services.keyService().has(player, crate.keys())) {
            String keyName = keyDisplayName(crate);
            lang.send(player, "open-no-key", Placeholder.parsed("key", keyName));
            knockback(player, placement, crate);
            return;
        }
        if (crate.rewardsMode() == RewardsMode.SELECTIVE) {
            new SelectiveMenu(player, crate, placement).open();
            return;
        }
        boolean quick = quickRequested && crate.open().quickOpen() && Services.get().config().quickOpenEnabled();
        double cost = crate.open().cost();
        if (cost > 0 && VaultHook.ready()) {
            if (!VaultHook.has(player, cost)) {
                lang.send(player, "open-not-enough-money",
                        Placeholder.unparsed("cost", VaultHook.format(cost)));
                return;
            }
            new ConfirmMenu(player, VaultHook.format(cost), () -> proceed(player, crate, placement, quick)).open();
            return;
        }
        proceed(player, crate, placement, quick);
    }

    /** Everything after checks/confirmation. Rolls first — abort paths cost nothing. */
    private void proceed(Player player, Crate crate, CratePlacement placement, boolean quick) {
        var services = Services.get();
        var lang = services.lang();

        if (placement != null && !crate.open().simultaneous() && !placement.lock()) {
            lang.send(player, "open-already-opening");
            return;
        }
        Rolled rolled = rollOutcome(player, crate, crate.maxWinRewards());
        if (rolled == null) {
            lang.send(player, "open-nothing-available");
            unlock(placement, crate);
            return;
        }

        CrateOpenEvent event = new CrateOpenEvent(player, crate, rolled.rewards);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            unlock(placement, crate);
            return;
        }

        if (!crate.keys().isEmpty() && !services.keyService().consume(player, crate.keys())) {
            lang.send(player, "open-no-key", Placeholder.parsed("key", keyDisplayName(crate)));
            unlock(placement, crate);
            return;
        }
        double cost = crate.open().cost();
        if (cost > 0 && VaultHook.ready()) {
            if (!VaultHook.withdraw(player, cost)) {
                lang.send(player, "open-not-enough-money", Placeholder.unparsed("cost", VaultHook.format(cost)));
                unlock(placement, crate);
                return;
            }
            lang.send(player, "open-cost-charged", Placeholder.unparsed("cost", VaultHook.format(cost)));
        }

        PlayerData data = services.players().of(player);
        if (crate.open().cooldownSeconds() > 0) {
            data.setCooldown(crate.id(), Instant.now().getEpochSecond() + crate.open().cooldownSeconds());
        }
        data.incrOpens(crate.id());
        if (rolled.milestoneIndex >= 0) data.setMilestoneIndex(crate.id(), rolled.milestoneIndex);
        if (rolled.guaranteed) {
            lang.send(player, "open-guaranteed",
                    Placeholder.parsed("reward", rolled.rewards.get(0).displayName()));
        }
        services.actionLogger().open(player.getName(), crate.id());

        int temporaryRerolls = services.rerolls().sessionAllowance(player, crate);
        OpenSession session = new OpenSession(player, crate, placement, quick,
                rolled.rewards, rolled.alternative, temporaryRerolls);
        services.animations().play(player, crate, placement, rolled.rewards, quick,
                () -> onAnimationComplete(session));
    }

    private void onAnimationComplete(OpenSession session) {
        var services = Services.get();
        boolean canReroll = session.crate().rerollEnabled()
                && session.outcome().size() == 1
                && !session.alternative(0)
                && services.rerolls().available(session.player(), session.crate(), session.temporaryRerolls()) > 0
                && session.player().isOnline();
        if (canReroll) {
            new RerollMenu(session).open();
        } else {
            session.grantAll();
        }
    }

    // --- rolling ---

    public record Rolled(List<Reward> rewards, boolean[] alternative, boolean guaranteed, int milestoneIndex) {
    }

    /**
     * Rolls {@code count} rewards. Rewards whose limits/permissions block them and that
     * have no alternative are excluded up front so configured promises hold; blocked
     * rewards with an alternative stay in the pool at their advertised odds. Rewards
     * flagged {@code always} never enter the pool — they are appended to every outcome
     * (the lootbox "guaranteed items" section).
     */
    public Rolled rollOutcome(Player player, Crate crate, int count) {
        var services = Services.get();
        PlayerData data = services.players().of(player);

        List<Reward> alwaysRewards = new ArrayList<>();
        List<Reward> pool = new ArrayList<>();
        for (Reward reward : crate.rewards().values()) {
            if (reward.always()) {
                alwaysRewards.add(reward);
                continue;
            }
            if (reward.percentage() <= 0) continue;
            if (isAllowed(player, data, crate, reward) || reward.alternative().enabled()) {
                pool.add(reward);
            }
        }
        if (pool.isEmpty() && alwaysRewards.isEmpty()) return null;

        List<Reward> rewards = new ArrayList<>();
        boolean guaranteed = false;
        int milestoneIndex = -1;

        if (crate.guaranteedEnabled() && !crate.milestones().isEmpty()) {
            int nextTotal = data.opens(crate.id()) + 1;
            GuaranteedWin.Result result = GuaranteedWin.check(crate.guaranteedMode(), crate.milestones(),
                    nextTotal, data.milestoneIndex(crate.id()), rng);
            if (result != null) {
                Reward forced = crate.reward(result.rewardId());
                if (forced != null) {
                    rewards.add(forced);
                    guaranteed = true;
                }
                milestoneIndex = result.nextIndex();
            }
        }
        while (!pool.isEmpty() && rewards.size() < count) {
            List<Reward> source = new ArrayList<>(pool);
            source.removeAll(rewards);
            if (source.isEmpty()) source = pool;
            Reward roll = rollEngine.roll(source, com.nocrates.reward.Weights::of);
            if (roll == null) break;
            rewards.add(roll);
        }
        for (Reward extra : alwaysRewards) {
            if (!rewards.contains(extra)) rewards.add(extra);
        }
        if (rewards.isEmpty()) return null;

        boolean[] alternative = new boolean[rewards.size()];
        for (int i = 0; i < rewards.size(); i++) {
            alternative[i] = !isAllowed(player, data, crate, rewards.get(i));
        }
        return new Rolled(rewards, alternative, guaranteed, milestoneIndex);
    }

    /** One replacement roll for the reroll feature; excludes the previous reward. */
    public Reward rollReplacement(Player player, Crate crate, Reward exclude) {
        Rolled rolled = rollOutcome(player, crate, 1);
        if (rolled == null) return null;
        if (rolled.rewards.get(0) != exclude || crate.rewards().size() <= 1) return rolled.rewards.get(0);
        // one retry to avoid handing back the identical reward too often
        Rolled retry = rollOutcome(player, crate, 1);
        return retry == null ? rolled.rewards.get(0) : retry.rewards.get(0);
    }

    public boolean isAllowed(Player player, PlayerData data, Crate crate, Reward reward) {
        for (String permission : reward.restrictedPermissions()) {
            if (player.hasPermission(permission)) return false;
        }
        return Services.get().winLimits().allows(data, crate, reward);
    }

    // --- helpers ---

    private void unlock(CratePlacement placement, Crate crate) {
        if (placement != null && !crate.open().simultaneous()) placement.unlock();
    }

    private String keyDisplayName(Crate crate) {
        if (crate.keys().isEmpty()) return "";
        var key = Services.get().keys().get(crate.keys().get(0).keyId());
        if (key == null) return crate.keys().get(0).keyId();
        return key.item().name() != null ? key.item().name() : key.id();
    }

    private void knockback(Player player, CratePlacement placement, Crate crate) {
        if (placement == null || !crate.open().knockback()) return;
        double strength = Services.get().config().knockback();
        if (strength <= 0) return;
        Location from = placement.location();
        if (from == null) return;
        Vector push = player.getLocation().toVector()
                .subtract(from.toVector().add(new Vector(0.5, 0, 0.5)));
        push.setY(0.1);
        if (push.lengthSquared() < 0.01) push = new Vector(0, 0.1, 0.4);
        player.setVelocity(push.normalize().multiply(strength).setY(0.25));
    }
}
