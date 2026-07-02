package com.nocrates.reward;

import com.nocrates.api.events.RewardWinEvent;
import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.hook.Hooks;
import com.nocrates.item.ItemSpec;
import com.nocrates.text.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Actually delivers a reward: win-items (dropped when the inventory is full, unless the
 * crate-claim module intercepts), win-commands, broadcast, share-online, logging, stats
 * and the RewardWinEvent. Alternative rewards run through the same path.
 */
public final class RewardGrant {

    /** Module hook: return true to take over full-inventory item delivery (crate-claim). */
    public interface OverflowHandler {
        boolean store(Player player, Crate crate, Reward reward);
    }

    private static OverflowHandler overflowHandler;

    private RewardGrant() {
    }

    public static void overflowHandler(OverflowHandler handler) {
        overflowHandler = handler;
    }

    /** Grants {@code reward} (or its alternative when {@code useAlternative}). */
    public static void grant(Player player, Crate crate, Reward reward, boolean useAlternative) {
        grant(player, crate, reward, useAlternative, false);
    }

    /** {@code quiet} suppresses the personal you-won chat line (mass-open summaries). */
    public static void grant(Player player, Crate crate, Reward reward, boolean useAlternative, boolean quiet) {
        var lang = Services.get().lang();
        if (useAlternative && reward.alternative().enabled()) {
            AlternativeReward alt = reward.alternative();
            if (!alt.virtualOnly() && alt.item() != null) {
                giveItems(player, crate, reward, List.of(alt.item()));
            }
            runCommands(player, alt.commands());
            String name = alt.displayName().isEmpty() ? reward.displayName() : alt.displayName();
            if (!quiet) lang.send(player, "open-you-won", Placeholder.parsed("reward", name));
            if (alt.broadcast()) broadcast(player, crate, name);
            Services.get().actionLogger().win(player.getName(), crate.id(), reward.id() + " (alternative)");
            Bukkit.getPluginManager().callEvent(new RewardWinEvent(player, crate, reward, true));
            return;
        }

        if (!reward.virtualReward()) {
            giveItems(player, crate, reward, reward.winItems());
        }
        runCommands(player, reward.winCommands());
        if (!quiet) lang.send(player, "open-you-won", Placeholder.parsed("reward", reward.displayName()));
        if (reward.broadcast()) broadcast(player, crate, reward.displayName());
        if (reward.shareOnline()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) runCommands(online, reward.winCommands());
            }
        }
        Services.get().actionLogger().win(player.getName(), crate.id(), reward.id());
        Bukkit.getPluginManager().callEvent(new RewardWinEvent(player, crate, reward, false));
    }

    private static void giveItems(Player player, Crate crate, Reward reward, List<ItemSpec> items) {
        // Decide claim-vs-deliver BEFORE handing anything over. Delivering part of a
        // reward and then storing the whole reward as a claim would duplicate the
        // delivered part (and addItem mutates its argument, so post-hoc compensation
        // removes the wrong amount). Conservative check: one empty slot per item.
        if (overflowHandler != null && emptySlots(player) < items.size()
                && overflowHandler.store(player, crate, reward)) {
            return;
        }
        for (ItemSpec spec : items) {
            ItemStack stack = spec.build();
            var leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) continue;
            Services.get().lang().send(player, "open-inventory-full");
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }
        }
    }

    private static int emptySlots(Player player) {
        int empty = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) empty++;
        }
        return empty;
    }

    private static void runCommands(Player player, List<String> commands) {
        for (String command : commands) {
            String cmd = Hooks.papiApply(player, command.replace("%player%", player.getName()));
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            final String run = cmd;
            // Console dispatch belongs on the global/main thread (grants may arrive
            // from a crate's region thread on Folia).
            com.nocrates.compat.Scheduling.run(Services.get().plugin(), null, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run));
        }
    }

    private static void broadcast(Player player, Crate crate, String rewardName) {
        String template = crate.broadcastMessage();
        if (template != null && template.isEmpty()) return; // explicitly disabled
        if (template != null) {
            Bukkit.getServer().sendMessage(Text.mm(template
                    .replace("%player%", player.getName())
                    .replace("%reward%", rewardName)
                    .replace("%crate%", crate.displayName())));
            return;
        }
        var lang = Services.get().lang();
        Bukkit.getServer().sendMessage(lang.raw("open-broadcast",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.parsed("reward", rewardName),
                Placeholder.parsed("crate", crate.displayName())));
    }
}
