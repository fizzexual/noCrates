package com.nocrates.command;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.message.Messages;
import com.nocrates.open.PreviewMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Player command: {@code /crate [name]} to open, {@code /crate preview <name>}, {@code /crate list}. */
public final class CrateCommand implements CommandExecutor, TabCompleter {

    private final NoCrates plugin;

    public CrateCommand(NoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.services().messages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listCrates(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("preview")) {
            if (args.length < 2) {
                messages.send(player, "usage", Messages.ph("usage", "/crate preview <crate>"));
                return true;
            }
            openPreview(player, args[1]);
            return true;
        }
        Crate crate = plugin.services().crates().get(args[0]);
        if (crate == null) {
            messages.send(player, "unknown-crate", Messages.ph("crate", args[0]));
            return true;
        }
        if (!player.hasPermission("nocrates.open")) {
            messages.send(player, "no-permission");
            return true;
        }
        plugin.services().openController().open(player, crate);
        return true;
    }

    private void openPreview(Player player, String name) {
        Messages messages = plugin.services().messages();
        Crate crate = plugin.services().crates().get(name);
        if (crate == null) {
            messages.send(player, "unknown-crate", Messages.ph("crate", name));
            return;
        }
        new PreviewMenu(crate, plugin.services().rarities()).open(player);
    }

    private void listCrates(Player player) {
        Messages messages = plugin.services().messages();
        var crates = plugin.services().crates().all();
        if (crates.isEmpty()) {
            messages.send(player, "no-crates");
            return;
        }
        String list = crates.stream()
                .map(c -> c.name() + " (" + plugin.services().keys().total(player, c) + ")")
                .collect(Collectors.joining(", "));
        messages.send(player, "crate-list",
                Messages.ph("count", String.valueOf(crates.size())),
                Messages.ph("crates", list));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(plugin.services().crates().names());
            options.add("preview");
            options.add("list");
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return filter(new ArrayList<>(plugin.services().crates().names()), args[1]);
        }
        return List.of();
    }

    static List<String> filter(List<String> options, String prefix) {
        String low = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(low)).collect(Collectors.toList());
    }
}
