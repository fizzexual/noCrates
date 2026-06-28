package com.nocrates.command;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CrateBlock;
import com.nocrates.key.KeyManager;
import com.nocrates.message.Messages;
import com.nocrates.open.PreviewMenu;
import com.nocrates.util.Locations;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Admin command tree: reload, list, give, key, open, preview, setblock. */
public final class CratesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS =
            List.of("reload", "list", "give", "key", "open", "preview", "setblock");
    private static final List<String> KEY_SUBS = List.of("give", "giveall", "take");

    private final NoCrates plugin;

    public CratesCommand(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.services().messages();
        if (!sender.hasPermission("nocrates.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.services().reload();
                messages.send(sender, "reload");
            }
            case "list" -> list(sender);
            case "give" -> give(sender, args);
            case "key" -> key(sender, args);
            case "open" -> open(sender, args);
            case "preview" -> preview(sender, args);
            case "setblock" -> setblock(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        Messages m = plugin.services().messages();
        if (args.length < 3) {
            m.send(sender, "usage", Messages.ph("usage", "/crates give [crate] [player] [amount]"));
            return;
        }
        Crate crate = plugin.services().crates().get(args[1]);
        if (crate == null) {
            m.send(sender, "unknown-crate", Messages.ph("crate", args[1]));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            m.send(sender, "player-not-found", Messages.ph("target", args[2]));
            return;
        }
        int amount = parseAmount(args, 3);
        ItemStack key = plugin.services().keys().physicalKey(crate, amount);
        target.getInventory().addItem(key).values()
                .forEach(rem -> target.getWorld().dropItemNaturally(target.getLocation(), rem));
        m.send(sender, "keys-given", Messages.ph("amount", String.valueOf(amount)),
                Messages.ph("crate", crate.name()), Messages.ph("target", target.getName()));
        m.send(target, "keys-received", Messages.ph("amount", String.valueOf(amount)),
                Messages.ph("crate", crate.name()));
    }

    private void key(CommandSender sender, String[] args) {
        Messages m = plugin.services().messages();
        if (args.length < 3) {
            m.send(sender, "usage", Messages.ph("usage", "/crates key give|giveall|take [crate] [player] [amount]"));
            return;
        }
        String sub = args[1].toLowerCase();
        Crate crate = plugin.services().crates().get(args[2]);
        if (crate == null) {
            m.send(sender, "unknown-crate", Messages.ph("crate", args[2]));
            return;
        }
        KeyManager keys = plugin.services().keys();
        switch (sub) {
            case "give" -> {
                if (args.length < 4) {
                    m.send(sender, "usage", Messages.ph("usage", "/crates key give [crate] [player] [amount]"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    m.send(sender, "player-not-found", Messages.ph("target", args[3]));
                    return;
                }
                int amount = parseAmount(args, 4);
                keys.giveVirtual(target.getUniqueId(), crate, amount);
                m.send(sender, "keys-given", Messages.ph("amount", String.valueOf(amount)),
                        Messages.ph("crate", crate.name()), Messages.ph("target", target.getName()));
                m.send(target, "keys-received", Messages.ph("amount", String.valueOf(amount)),
                        Messages.ph("crate", crate.name()));
            }
            case "giveall" -> {
                int amount = parseAmount(args, 3);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    keys.giveVirtual(p.getUniqueId(), crate, amount);
                    m.send(p, "keys-received", Messages.ph("amount", String.valueOf(amount)),
                            Messages.ph("crate", crate.name()));
                }
                m.send(sender, "keys-given-all", Messages.ph("amount", String.valueOf(amount)),
                        Messages.ph("crate", crate.name()));
            }
            case "take" -> {
                if (args.length < 4) {
                    m.send(sender, "usage", Messages.ph("usage", "/crates key take [crate] [player] [amount]"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    m.send(sender, "player-not-found", Messages.ph("target", args[3]));
                    return;
                }
                int amount = parseAmount(args, 4);
                keys.takeVirtual(target.getUniqueId(), crate, amount);
                m.send(sender, "keys-taken", Messages.ph("amount", String.valueOf(amount)),
                        Messages.ph("crate", crate.name()), Messages.ph("target", target.getName()));
            }
            default -> m.send(sender, "usage",
                    Messages.ph("usage", "/crates key give|giveall|take [crate] [player] [amount]"));
        }
    }

    private void open(CommandSender sender, String[] args) {
        Messages m = plugin.services().messages();
        if (args.length < 2) {
            m.send(sender, "usage", Messages.ph("usage", "/crates open [crate] [player]"));
            return;
        }
        Crate crate = plugin.services().crates().get(args[1]);
        if (crate == null) {
            m.send(sender, "unknown-crate", Messages.ph("crate", args[1]));
            return;
        }
        Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2])
                : (sender instanceof Player p ? p : null);
        if (target == null) {
            m.send(sender, "player-not-found", Messages.ph("target", args.length >= 3 ? args[2] : "?"));
            return;
        }
        plugin.services().openController().open(target, crate);
    }

    private void preview(CommandSender sender, String[] args) {
        Messages m = plugin.services().messages();
        if (!(sender instanceof Player player)) {
            m.send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            m.send(sender, "usage", Messages.ph("usage", "/crates preview [crate]"));
            return;
        }
        Crate crate = plugin.services().crates().get(args[1]);
        if (crate == null) {
            m.send(sender, "unknown-crate", Messages.ph("crate", args[1]));
            return;
        }
        new PreviewMenu(crate, plugin.services().rarities()).open(player);
    }

    private void setblock(CommandSender sender, String[] args) {
        Messages m = plugin.services().messages();
        if (!(sender instanceof Player player)) {
            m.send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            m.send(sender, "usage", Messages.ph("usage", "/crates setblock [crate]"));
            return;
        }
        Crate crate = plugin.services().crates().get(args[1]);
        if (crate == null) {
            m.send(sender, "unknown-crate", Messages.ph("crate", args[1]));
            return;
        }
        Block block = player.getTargetBlockExact(6);
        if (block == null) {
            m.send(sender, "no-target-block");
            return;
        }
        List<String> locations = new ArrayList<>(crate.block() != null ? crate.block().locations() : List.of());
        String loc = Locations.serialize(block.getLocation());
        if (!locations.contains(loc)) {
            locations.add(loc);
        }
        CrateBlock old = crate.block();
        CrateBlock updated = new CrateBlock(true, locations,
                old != null && !old.hologram().isEmpty() ? old.hologram()
                        : List.of("<bold>" + crate.name(), "<gray>Right-click to open"),
                old != null ? old.particle() : null);
        plugin.services().crates().save(rebuildWithBlock(crate, updated));
        plugin.services().crateBlocks().refresh();
        m.send(sender, "crate-bound", Messages.ph("crate", crate.name()));
    }

    private Crate rebuildWithBlock(Crate crate, CrateBlock block) {
        return Crate.builder(crate.name())
                .displayName(crate.displayName())
                .animation(crate.animation())
                .key(crate.key())
                .block(block)
                .preview(crate.previewEnabled(), crate.previewTitle())
                .pity(crate.pity())
                .broadcast(crate.broadcast())
                .cooldownSeconds(crate.cooldownSeconds())
                .openSound(crate.openSound())
                .rewards(crate.rewards())
                .build();
    }

    private void list(CommandSender sender) {
        Messages m = plugin.services().messages();
        var names = plugin.services().crates().names();
        if (names.isEmpty()) {
            m.send(sender, "no-crates");
            return;
        }
        m.send(sender, "crate-list", Messages.ph("count", String.valueOf(names.size())),
                Messages.ph("crates", String.join(", ", names)));
    }

    private void help(CommandSender sender) {
        plugin.services().messages().sendRaw(sender, "admin-help");
    }

    private int parseAmount(String[] args, int index) {
        if (args.length > index) {
            try {
                return Math.max(1, Integer.parseInt(args[index]));
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 1;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nocrates.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return CrateCommand.filter(new ArrayList<>(SUBS), args[0]);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("key")) {
                return CrateCommand.filter(new ArrayList<>(KEY_SUBS), args[1]);
            }
            return CrateCommand.filter(new ArrayList<>(plugin.services().crates().names()), args[1]);
        }
        if (args.length == 3) {
            if (sub.equals("key")) {
                return CrateCommand.filter(new ArrayList<>(plugin.services().crates().names()), args[2]);
            }
            if (sub.equals("give") || sub.equals("open")) {
                return CrateCommand.filter(onlinePlayerNames(), args[2]);
            }
        }
        if (args.length == 4 && sub.equals("key")
                && (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("take"))) {
            return CrateCommand.filter(onlinePlayerNames(), args[3]);
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }
}
