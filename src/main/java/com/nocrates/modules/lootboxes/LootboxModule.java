package com.nocrates.modules.lootboxes;

import com.nocrates.api.Addon;
import com.nocrates.api.events.CrateOpenEvent;
import com.nocrates.compat.Compat;
import com.nocrates.compat.Scheduling;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CratePlacement;
import com.nocrates.crate.Loc;
import com.nocrates.item.ItemBuilder;
import com.nocrates.open.OpenSession;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RewardGrant;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * LootBoxes module — two flavours of inventory-carried crates:
 *
 * <b>Right-click lootboxes</b> (the default): a held item whose lore lists the
 * guaranteed items and the full reward pool ("2x Legendary Money Pouch" style).
 * Right-clicking consumes one and instantly grants the crate's always-rewards plus
 * max-win-rewards random rolls, with a chat summary.
 *
 * <b>Placeable lootboxes</b>: place the box, the crate's animation plays once,
 * the reward is granted and the block disappears.
 */
public final class LootboxModule extends Addon implements Listener {

    public static final NamespacedKey PDC_LOOTBOX = NamespacedKey.fromString("nocrates:lootbox");
    public static final NamespacedKey PDC_LOOTBOX_RC = NamespacedKey.fromString("nocrates:lootbox_rc");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, api().plugin());
        api().registerCommand("lootbox", "nocrates.command.givecrate", (sender, args) -> {
            if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
                api().lang().send(sender, "unknown-command");
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
            boolean placeable = args.length >= 5 && args[4].equalsIgnoreCase("placeable");
            ItemStack box = placeable ? placeableItem(crate, amount) : rightClickItem(crate, amount);
            target.getInventory().addItem(box).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            api().lang().send(sender, "crate-given",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("crate", crate.id() + (placeable ? " (placeable lootbox)" : " (lootbox)")),
                    Placeholder.unparsed("player", target.getName()));
        }, (sender, args) -> switch (args.length) {
            case 1 -> List.of("give");
            case 2 -> new ArrayList<>(api().crates().ids());
            case 3 -> com.nocrates.command.CratesCommand.playerNames();
            case 4 -> com.nocrates.command.CratesCommand.amounts();
            case 5 -> List.of("placeable");
            default -> List.of();
        });
    }

    private static int parse(String s) {
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ------------------------------------------------------------------
    // Right-click lootboxes
    // ------------------------------------------------------------------

    /**
     * Builds the redeemable item. The lore is generated from the crate: hint lines,
     * then a "guaranteed" section (always-rewards) and a "possible rewards" section
     * with the amount each reward gives.
     */
    public ItemStack rightClickItem(Crate crate, int amount) {
        var lang = api().lang();
        List<String> lore = new ArrayList<>();
        lore.add(lang.rawString("lootbox-lore-hint-1"));
        lore.add(lang.rawString("lootbox-lore-hint-2")
                .replace("<count>", String.valueOf(crate.maxWinRewards())));

        List<Reward> always = new ArrayList<>();
        List<Reward> pool = new ArrayList<>();
        for (Reward reward : crate.rewards().values()) {
            if (reward.always()) always.add(reward);
            else if (reward.percentage() > 0) pool.add(reward);
        }
        if (!always.isEmpty()) {
            lore.add("");
            lore.add(lang.rawString("lootbox-lore-guaranteed"));
            for (Reward reward : always) lore.add(entryLine(reward));
        }
        if (!pool.isEmpty()) {
            lore.add("");
            lore.add(lang.rawString("lootbox-lore-rewards"));
            for (Reward reward : pool) lore.add(entryLine(reward));
        }
        Material material = Compat.material(crate.blockMaterial(), Material.CHEST);
        return ItemBuilder.of(material)
                .name(lang.rawString("lootbox-item-name").replace("<name>", crate.displayName()))
                .lore(lore)
                .glow(true)
                .amount(amount)
                .pdc(PDC_LOOTBOX_RC, crate.id())
                .build();
    }

    /** " • 2x Legendary Money Pouch" — amount comes from the first win-item. */
    private String entryLine(Reward reward) {
        int amount = reward.winItems().isEmpty() ? 1 : reward.winItems().get(0).amount();
        return api().lang().rawString("lootbox-lore-entry")
                .replace("<amount>", String.valueOf(amount))
                .replace("<name>", reward.displayName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUse(PlayerInteractEvent event) {
        // both hands: an off-hand lootbox must be intercepted too, otherwise vanilla
        // places it as a plain block and the redeemable item is destroyed
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String crateId = meta.getPersistentDataContainer().get(PDC_LOOTBOX_RC, PersistentDataType.STRING);
        if (crateId == null) return;
        // clicking an actual placed crate wins over the held lootbox
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && api().placements().at(event.getClickedBlock()) != null) {
            event.setCancelled(true); // ...but never place the lootbox against it
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        Crate crate = api().crates().get(crateId);
        if (crate == null) {
            api().lang().send(player, "crate-not-found", Placeholder.unparsed("crate", crateId));
            return;
        }
        redeem(player, crate, item);
    }

    /** Consumes one box and instantly grants always-rewards + the random rolls. */
    private void redeem(Player player, Crate crate, ItemStack item) {
        var services = com.nocrates.core.Services.get();
        if (!crate.enabled()) {
            api().lang().send(player, "open-crate-disabled");
            return;
        }
        if (crate.permissionRequired() && !player.hasPermission(crate.permission())) {
            api().lang().send(player, "open-no-crate-permission");
            return;
        }
        var rolled = api().openService().rollOutcome(player, crate, crate.maxWinRewards());
        if (rolled == null) {
            api().lang().send(player, "open-nothing-available");
            return;
        }
        CrateOpenEvent event = new CrateOpenEvent(player, crate, rolled.rewards());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        api().openService().recomputeAlternatives(player, crate, rolled);

        item.setAmount(item.getAmount() - 1);
        var data = api().players().of(player);
        data.incrOpens(crate.id());
        services.actionLogger().open(player.getName(), crate.id() + " (lootbox)");

        api().lang().send(player, "lootbox-opened", Placeholder.parsed("crate", crate.displayName()));
        for (int i = 0; i < rolled.rewards().size(); i++) {
            Reward reward = rolled.rewards().get(i);
            boolean alternative = i < rolled.alternative().length && rolled.alternative()[i];
            if (!alternative) services.winLimits().record(data, crate, reward);
            RewardGrant.grant(player, crate, reward, alternative, true);
            int amount = reward.winItems().isEmpty() ? 1 : reward.winItems().get(0).amount();
            api().lang().sendRaw(player, "massopen-summary-line",
                    Placeholder.parsed("reward", reward.displayName()),
                    Placeholder.unparsed("amount", String.valueOf(amount)));
        }
        Compat.play(player, "ENTITY_PLAYER_LEVELUP", 0.9f, 1.3f);
        Compat.spawn(Compat.particle("TOTEM_OF_UNDYING"), player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.15);
    }

    // ------------------------------------------------------------------
    // Placeable lootboxes (the classic style)
    // ------------------------------------------------------------------

    public ItemStack placeableItem(Crate crate, int amount) {
        Material material = Compat.material(crate.blockMaterial(), Material.CHEST);
        return ItemBuilder.of(material)
                .name(crate.displayName())
                .lore(new ArrayList<>(List.of(api().lang().rawString("lootbox-lore"))))
                .glow(true)
                .amount(amount)
                .pdc(PDC_LOOTBOX, crate.id())
                .build();
    }

    /** Placed lootboxes currently mid-animation: locKey -> session (for guards + shutdown). */
    private final java.util.Map<String, OpenSession> activePlacements = new java.util.concurrent.ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        // a right-click lootbox must never end up placed as a plain block
        if (meta.getPersistentDataContainer().has(PDC_LOOTBOX_RC, PersistentDataType.STRING)) {
            event.setCancelled(true);
            return;
        }
        String crateId = meta.getPersistentDataContainer().get(PDC_LOOTBOX, PersistentDataType.STRING);
        if (crateId == null) return;
        Crate crate = api().crates().get(crateId);
        if (crate == null || !crate.enabled()) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location blockLoc = block.getLocation();
        Material placedType = block.getType();
        // Defer one tick: if a later-priority handler (protection plugin) cancelled the
        // place, the block is gone by then — no roll, no grant, box already refunded.
        Scheduling.later(api().plugin(), blockLoc, 1, () -> {
            if (blockLoc.getBlock().getType() != placedType) return;
            startPlacedOpen(player, crate, blockLoc);
        });
    }

    private void startPlacedOpen(Player player, Crate crate, Location blockLoc) {
        var rolled = api().openService().rollOutcome(player, crate, crate.maxWinRewards());
        if (rolled == null) {
            api().lang().send(player, "open-nothing-available");
            blockLoc.getBlock().setType(Material.AIR, false);
            return;
        }
        CrateOpenEvent event = new CrateOpenEvent(player, crate, rolled.rewards());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            blockLoc.getBlock().setType(Material.AIR, false);
            return;
        }
        api().openService().recomputeAlternatives(player, crate, rolled);
        var data = api().players().of(player);
        data.incrOpens(crate.id());
        com.nocrates.core.Services.get().actionLogger().open(player.getName(), crate.id() + " (lootbox)");
        String locKey = Loc.key(blockLoc);
        CratePlacement temporary = new CratePlacement(crate, locKey);
        OpenSession session = new OpenSession(player, crate, null, false,
                rolled.rewards(), rolled.alternative(), 0);
        activePlacements.put(locKey, session);
        api().animations().play(player, crate, temporary, rolled.rewards(), false, () -> {
            session.grantAll();
            activePlacements.remove(locKey);
            Scheduling.run(api().plugin(), blockLoc, () -> {
                if (blockLoc.getBlock().getType() != Material.AIR) {
                    blockLoc.getBlock().setType(Material.AIR, false);
                }
            });
        });
    }

    /** The placed box can't be mined mid-animation. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (activePlacements.containsKey(Loc.key(event.getBlock()))) event.setCancelled(true);
    }

    @Override
    public void onDisable() {
        // shutdown mid-animation: grant everything owed and clean the blocks up
        for (var entry : new java.util.HashMap<>(activePlacements).entrySet()) {
            entry.getValue().grantAll();
            Location loc = Loc.parse(entry.getKey());
            if (loc != null) {
                try {
                    loc.getBlock().setType(Material.AIR, false);
                } catch (Exception ignored) {
                }
            }
        }
        activePlacements.clear();
    }
}
