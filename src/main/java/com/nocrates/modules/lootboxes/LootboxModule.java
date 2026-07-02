package com.nocrates.modules.lootboxes;

import com.nocrates.api.Addon;
import com.nocrates.compat.Scheduling;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.crate.Loc;
import com.nocrates.item.ItemBuilder;
import com.nocrates.open.OpenSession;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * LootBoxes (PhoenixCrates add-on parity): inventory-carried crates. Give a player a
 * lootbox item; placing it plays the crate's animation once, grants a reward and the
 * box disappears. No key needed.
 */
public final class LootboxModule extends Addon implements Listener {

    public static final NamespacedKey PDC_LOOTBOX = NamespacedKey.fromString("nocrates:lootbox");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, api().plugin());
        api().registerCommand("lootbox", (sender, args) -> {
            if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
                api().lang().send(sender, "unknown-command");
                return;
            }
            if (!sender.hasPermission("nocrates.command.givecrate") && !sender.hasPermission("nocrates.admin")) {
                api().lang().send(sender, "no-permission");
                return;
            }
            Crate crate = api().crates().get(args[1]);
            if (crate == null) {
                api().lang().send(sender, "crate-not-found", Placeholder.unparsed("crate", args[1]));
                return;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                api().lang().send(sender, "player-not-found", Placeholder.unparsed("name", args[2]));
                return;
            }
            int amount = args.length >= 4 ? parse(args[3]) : 1;
            target.getInventory().addItem(lootboxItem(crate, amount)).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            api().lang().send(sender, "crate-given",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("crate", crate.id() + " (lootbox)"),
                    Placeholder.unparsed("player", target.getName()));
        });
    }

    private static int parse(String s) {
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public ItemStack lootboxItem(Crate crate, int amount) {
        Material material = com.nocrates.compat.Compat.material(crate.blockMaterial(), Material.CHEST);
        List<String> lore = new ArrayList<>(List.of(api().lang().rawString("lootbox-lore")));
        ItemStack item = ItemBuilder.of(material)
                .name(crate.displayName())
                .lore(lore)
                .glow(true)
                .amount(amount)
                .pdc(PDC_LOOTBOX, crate.id())
                .build();
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String crateId = meta.getPersistentDataContainer().get(PDC_LOOTBOX, PersistentDataType.STRING);
        if (crateId == null) return;
        Crate crate = api().crates().get(crateId);
        if (crate == null) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        // Roll now (keyless by design); if nothing is available refund by cancelling.
        var rolled = api().openService().rollOutcome(player, crate, crate.maxWinRewards());
        if (rolled == null) {
            api().lang().send(player, "open-nothing-available");
            event.setCancelled(true);
            return;
        }
        var data = api().players().of(player);
        data.incrOpens(crate.id());
        logOpen(player, crate);
        CratePlacement temporary = new CratePlacement(crate, Loc.key(block));
        OpenSession session = new OpenSession(player, crate, null, false,
                rolled.rewards(), rolled.alternative(), 0);
        Location blockLoc = block.getLocation();
        api().animations().play(player, crate, temporary, rolled.rewards(), false, () -> {
            session.grantAll();
            Scheduling.run(api().plugin(), blockLoc, () -> {
                if (blockLoc.getBlock().getType() != Material.AIR) {
                    blockLoc.getBlock().setType(Material.AIR, false);
                }
            });
        });
    }

    private void logOpen(Player player, Crate crate) {
        com.nocrates.core.Services.get().actionLogger().open(player.getName(), crate.id() + " (lootbox)");
    }
}
