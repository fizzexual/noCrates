package com.nocrates.modules.crateclaim;

import com.nocrates.api.Addon;
import com.nocrates.reward.RewardGrant;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.time.Instant;

/**
 * Crate Claim module: rewards that don't fit the winner's
 * inventory are stored instead of dropped, claimable any time with /crates claim.
 */
public final class CrateClaimModule extends Addon {

    @Override
    public void onEnable() {
        RewardGrant.overflowHandler((player, crate, reward) -> {
            api().players().of(player).addClaim(crate.id() + ";" + reward.id() + ";" + Instant.now().getEpochSecond());
            api().lang().send(player, "claim-stored", Placeholder.unparsed("count", "1"));
            return true;
        });
        api().registerCommand("claim", (sender, args) -> {
            if (!(sender instanceof Player player)) {
                api().lang().send(sender, "player-only");
                return;
            }
            if (!player.hasPermission("nocrates.claim")) {
                api().lang().send(sender, "no-permission");
                return;
            }
            new ClaimMenu(player, api()).open();
        });
    }

    @Override
    public void onDisable() {
        RewardGrant.overflowHandler(null);
    }
}
