package com.nocrates.command;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CrateClickListener;
import com.nocrates.crate.CratePlacement;
import com.nocrates.crate.Loc;
import com.nocrates.key.Key;
import com.nocrates.open.OpenSession;
import com.nocrates.open.PreviewMenu;
import com.nocrates.open.VirtualKeysMenu;
import com.nocrates.reward.Reward;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * /crates command tree (aliases: /crate, /nocrates, /nc). Modules and the editor
 * register extra verbs at enable time via {@link #registerExtra}.
 */
public final class CratesCommand implements TabExecutor {

    private static final Map<String, BiConsumer<CommandSender, String[]>> EXTRA = new LinkedHashMap<>();

    /** Lets modules add verbs like "claim", "massopen", "editor", "migrate". */
    public static void registerExtra(String verb, BiConsumer<CommandSender, String[]> handler) {
        EXTRA.put(verb.toLowerCase(Locale.ROOT), handler);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var services = Services.get();
        var lang = services.lang();
        if (args.length == 0) {
            return help(sender);
        }
        String verb = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        BiConsumer<CommandSender, String[]> extra = EXTRA.get(verb);
        if (extra != null) {
            extra.accept(sender, rest);
            return true;
        }

        switch (verb) {
            case "help" -> help(sender);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "open" -> open(sender, rest);
            case "preview" -> preview(sender, rest);
            case "virtualkeys" -> virtualKeys(sender);
            case "create" -> create(sender, rest);
            case "delete" -> delete(sender, rest);
            case "clone" -> cloneCrate(sender, rest);
            case "enable" -> setEnabled(sender, rest, true);
            case "disable" -> setEnabled(sender, rest, false);
            case "givecrate" -> giveCrate(sender, rest);
            case "placecrate" -> placeCrate(sender, rest);
            case "attach" -> attach(sender, rest);
            case "detach" -> detach(sender);
            case "givereward" -> giveReward(sender, rest, false);
            case "giverandomreward" -> giveReward(sender, rest, true);
            case "key" -> key(sender, rest);
            case "reroll" -> reroll(sender, rest);
            case "resetcooldown" -> resetCooldown(sender, rest);
            case "resetwinlimit" -> resetWinLimit(sender, rest);
            case "stats" -> stats(sender, rest);
            default -> lang.send(sender, EXTRA.isEmpty() || !isModuleVerb(verb) ? "unknown-command" : "module-disabled");
        }
        return true;
    }

    private boolean isModuleVerb(String verb) {
        return List.of("claim", "massopen", "migrate", "editor", "edit").contains(verb);
    }

    // --- player verbs ---

    private boolean help(CommandSender sender) {
        var lang = Services.get().lang();
        lang.send(sender, "help-header");
        String[][] lines = {
                {"/crates open <crate>", "Open a crate"},
                {"/crates preview <crate>", "Preview rewards & chances"},
                {"/crates virtualkeys", "Your virtual keys"},
                {"/crates claim", "Claim stored rewards"},
                {"/crates list", "List crates"},
                {"/crates editor", "In-game editor"},
                {"/crates key give|giveall|take|set|check|pay ...", "Manage keys"},
                {"/crates givecrate|placecrate|attach|detach", "Place crates"},
                {"/crates reload", "Reload configuration"},
        };
        for (String[] line : lines) {
            lang.sendRaw(sender, "help-line",
                    Placeholder.unparsed("command", line[0]),
                    Placeholder.unparsed("description", line[1]));
        }
        return true;
    }

    private void list(CommandSender sender) {
        if (denied(sender, "nocrates.command.list")) return;
        sender.sendMessage(com.nocrates.text.Text.mm("<gray>Crates: <white>"
                + String.join("<gray>, <white>", Services.get().crates().ids())));
    }

    private void reload(CommandSender sender) {
        if (denied(sender, "nocrates.command.reload")) return;
        var services = Services.get();
        services.config().reload();
        services.lang().load(services.config().language());
        services.keys().reload();
        services.crates().reload();
        services.reloads().reloadAll();
        services.placements().rebuild();
        for (Crate crate : services.crates().all()) services.winLimits().warm(crate.id());
        services.lang().send(sender, "reload-done");
    }

    private void open(CommandSender sender, String[] args) {
        var services = Services.get();
        if (args.length == 0) {
            services.lang().send(sender, "unknown-command");
            return;
        }
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        Player target;
        if (args.length >= 2) {
            if (denied(sender, "nocrates.command.open")) return;
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                services.lang().send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            services.lang().send(sender, "player-only");
            return;
        }
        services.openService().attempt(target, crate, nearestPlacement(crate, target), false);
    }

    private void preview(CommandSender sender, String[] args) {
        var services = Services.get();
        if (args.length == 0 || !(sender instanceof Player) && args.length < 2) {
            if (!(sender instanceof Player)) {
                services.lang().send(sender, "player-only");
                return;
            }
        }
        Crate crate = crateOrMsg(sender, args.length > 0 ? args[0] : "");
        if (crate == null) return;
        Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1])
                : sender instanceof Player self ? self : null;
        if (target == null) {
            services.lang().send(sender, "player-only");
            return;
        }
        new PreviewMenu(target, crate).open();
    }

    private void virtualKeys(CommandSender sender) {
        if (sender instanceof Player player) {
            new VirtualKeysMenu(player).open();
        } else {
            Services.get().lang().send(sender, "player-only");
        }
    }

    // --- crate admin ---

    private void create(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.create") || args.length == 0) return;
        var services = Services.get();
        String id = args[0].toLowerCase(Locale.ROOT);
        if (services.crates().exists(id)) {
            services.lang().send(sender, "crate-already-exists", Placeholder.unparsed("crate", id));
            return;
        }
        services.crates().create(id);
        services.lang().send(sender, "crate-created", Placeholder.unparsed("crate", id));
    }

    private void delete(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.delete") || args.length == 0) return;
        var services = Services.get();
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        services.crates().delete(crate.id());
        services.placements().rebuild();
        services.lang().send(sender, "crate-deleted", Placeholder.unparsed("crate", crate.id()));
    }

    private void cloneCrate(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.create") || args.length < 2) return;
        var services = Services.get();
        Crate source = crateOrMsg(sender, args[0]);
        if (source == null) return;
        if (services.crates().exists(args[1])) {
            services.lang().send(sender, "crate-already-exists", Placeholder.unparsed("crate", args[1]));
            return;
        }
        services.crates().clone(source, args[1]);
        services.lang().send(sender, "crate-cloned",
                Placeholder.unparsed("from", source.id()), Placeholder.unparsed("to", args[1].toLowerCase(Locale.ROOT)));
    }

    private void setEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (denied(sender, "nocrates.command.edit") || args.length == 0) return;
        var services = Services.get();
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        crate.enabled(enabled);
        services.crates().save(crate);
        services.lang().send(sender, enabled ? "crate-enabled" : "crate-disabled-msg",
                Placeholder.unparsed("crate", crate.id()));
    }

    private void giveCrate(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.givecrate") || args.length < 2) return;
        var services = Services.get();
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            services.lang().send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        int amount = args.length >= 3 ? parseInt(args[2], 1) : 1;
        target.getInventory().addItem(CrateClickListener.crateItem(crate, amount));
        services.lang().send(sender, "crate-given",
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("crate", crate.id()),
                Placeholder.unparsed("player", target.getName()));
    }

    private void placeCrate(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.edit") || args.length < 5) return;
        var services = Services.get();
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            services.lang().send(sender, "crate-not-looking");
            return;
        }
        Location loc = new Location(world, parseInt(args[2], 0), parseInt(args[3], 64), parseInt(args[4], 0));
        services.placements().attach(crate, loc.getBlock());
        services.lang().send(sender, "crate-placed",
                Placeholder.unparsed("crate", crate.id()),
                Placeholder.unparsed("location", Loc.key(loc)));
    }

    private void attach(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.edit") || args.length == 0) return;
        var services = Services.get();
        if (!(sender instanceof Player player)) {
            services.lang().send(sender, "player-only");
            return;
        }
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        Block block = player.getTargetBlockExact(6);
        if (block == null || block.getType().isAir()) {
            services.lang().send(sender, "crate-not-looking");
            return;
        }
        services.placements().attach(crate, block);
        services.lang().send(sender, "crate-attached", Placeholder.unparsed("crate", crate.id()));
    }

    private void detach(CommandSender sender) {
        if (denied(sender, "nocrates.command.edit")) return;
        var services = Services.get();
        if (!(sender instanceof Player player)) {
            services.lang().send(sender, "player-only");
            return;
        }
        Block block = player.getTargetBlockExact(6);
        if (block == null || services.placements().at(block) == null) {
            services.lang().send(sender, "crate-not-attached");
            return;
        }
        services.placements().detach(block);
        services.lang().send(sender, "crate-detached");
    }

    private void giveReward(CommandSender sender, String[] args, boolean random) {
        if (denied(sender, "nocrates.command.givereward")) return;
        var services = Services.get();
        int need = random ? 2 : 3;
        if (args.length < need) return;
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        String targetName = random ? args[1] : args[2];
        List<Player> targets = targetName.equalsIgnoreCase("all")
                ? new ArrayList<>(Bukkit.getOnlinePlayers())
                : Bukkit.getPlayerExact(targetName) != null
                ? List.of(Bukkit.getPlayerExact(targetName)) : List.of();
        if (targets.isEmpty()) {
            services.lang().send(sender, "player-not-found", Placeholder.unparsed("name", targetName));
            return;
        }
        for (Player target : targets) {
            Reward reward;
            if (random) {
                var rolled = services.openService().rollOutcome(target, crate, 1);
                if (rolled == null) continue;
                reward = rolled.rewards().get(0);
            } else {
                reward = crate.reward(args[1]);
                if (reward == null) {
                    services.lang().send(sender, "reward-not-found", Placeholder.unparsed("reward", args[1]));
                    return;
                }
            }
            OpenSession session = new OpenSession(target, crate, null, true,
                    new ArrayList<>(List.of(reward)), new boolean[]{false}, 0);
            session.grantAll();
            services.lang().send(sender, "reward-given",
                    Placeholder.unparsed("reward", reward.id()),
                    Placeholder.unparsed("crate", crate.id()),
                    Placeholder.unparsed("player", target.getName()));
        }
    }

    // --- keys ---

    private void key(CommandSender sender, String[] args) {
        var services = Services.get();
        var lang = services.lang();
        if (args.length == 0) {
            lang.send(sender, "unknown-command");
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give", "take", "set" -> {
                if (denied(sender, "nocrates.command.givekey") || args.length < 3) return;
                Key key = keyOrMsg(sender, args[1]);
                if (key == null) return;
                var target = Bukkit.getOfflinePlayerIfCached(args[2]);
                Player online = Bukkit.getPlayerExact(args[2]);
                if (target == null && online == null) {
                    lang.send(sender, "player-not-found", Placeholder.unparsed("name", args[2]));
                    return;
                }
                java.util.UUID id = online != null ? online.getUniqueId() : target.getUniqueId();
                int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;
                boolean physical = args.length >= 5 && args[4].equalsIgnoreCase("physical");
                switch (sub) {
                    case "give" -> {
                        if (physical && online != null && !key.virtual()) {
                            online.getInventory().addItem(key.physical(amount)).values()
                                    .forEach(left -> online.getWorld().dropItemNaturally(online.getLocation(), left));
                        } else {
                            services.keyService().give(id, key.id(), amount);
                        }
                        lang.send(sender, "key-given",
                                Placeholder.unparsed("amount", String.valueOf(amount)),
                                Placeholder.unparsed("key", key.id()),
                                Placeholder.unparsed("player", args[2]));
                        if (online != null) {
                            lang.send(online, "key-received",
                                    Placeholder.unparsed("amount", String.valueOf(amount)),
                                    Placeholder.unparsed("key", key.id()));
                        }
                    }
                    case "take" -> {
                        services.keyService().set(id, key.id(),
                                Math.max(0, (online != null ? services.players().of(online).keys(key.id()) : amount) - amount));
                        lang.send(sender, "key-taken",
                                Placeholder.unparsed("amount", String.valueOf(amount)),
                                Placeholder.unparsed("key", key.id()),
                                Placeholder.unparsed("player", args[2]));
                    }
                    case "set" -> {
                        services.keyService().set(id, key.id(), amount);
                        lang.send(sender, "key-set",
                                Placeholder.unparsed("player", args[2]),
                                Placeholder.unparsed("key", key.id()),
                                Placeholder.unparsed("amount", String.valueOf(amount)));
                    }
                    default -> {
                    }
                }
            }
            case "giveall" -> {
                if (denied(sender, "nocrates.command.givekey") || args.length < 2) return;
                Key key = keyOrMsg(sender, args[1]);
                if (key == null) return;
                int amount = args.length >= 3 ? parseInt(args[2], 1) : 1;
                var online = Bukkit.getOnlinePlayers();
                for (Player player : online) {
                    services.keyService().give(player.getUniqueId(), key.id(), amount);
                    lang.send(player, "key-received",
                            Placeholder.unparsed("amount", String.valueOf(amount)),
                            Placeholder.unparsed("key", key.id()));
                }
                lang.send(sender, "key-given-all",
                        Placeholder.unparsed("amount", String.valueOf(amount)),
                        Placeholder.unparsed("key", key.id()),
                        Placeholder.unparsed("count", String.valueOf(online.size())));
            }
            case "check" -> {
                if (denied(sender, "nocrates.command.givekey") || args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    lang.send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
                    return;
                }
                lang.send(sender, "key-balance-header", Placeholder.unparsed("player", target.getName()));
                for (Key key : services.keys().all()) {
                    int virtual = services.players().of(target).keys(key.id());
                    int physical = services.keyService().countPhysical(target, key.id());
                    if (virtual + physical == 0) continue;
                    lang.sendRaw(sender, "key-balance-line",
                            Placeholder.unparsed("key", key.id()),
                            Placeholder.unparsed("amount", String.valueOf(virtual + physical)),
                            Placeholder.unparsed("virtual", String.valueOf(virtual)),
                            Placeholder.unparsed("physical", String.valueOf(physical)));
                }
            }
            case "pay" -> {
                if (!(sender instanceof Player from)) {
                    lang.send(sender, "player-only");
                    return;
                }
                if (args.length < 3) return;
                Key key = keyOrMsg(sender, args[1]);
                if (key == null) return;
                Player to = Bukkit.getPlayerExact(args[2]);
                if (to == null) {
                    lang.send(sender, "player-not-found", Placeholder.unparsed("name", args[2]));
                    return;
                }
                int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;
                if (from.getUniqueId().equals(to.getUniqueId())) {
                    lang.send(sender, "key-pay-self");
                    return;
                }
                if (!services.keyService().pay(from, to.getUniqueId(), key.id(), amount)) {
                    lang.send(sender, "key-pay-insufficient");
                    return;
                }
                lang.send(from, "key-paid",
                        Placeholder.unparsed("amount", String.valueOf(amount)),
                        Placeholder.unparsed("key", key.id()),
                        Placeholder.unparsed("player", to.getName()));
                lang.send(to, "key-pay-received",
                        Placeholder.unparsed("player", from.getName()),
                        Placeholder.unparsed("amount", String.valueOf(amount)),
                        Placeholder.unparsed("key", key.id()));
            }
            default -> lang.send(sender, "unknown-command");
        }
    }

    private void reroll(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.givereroll") || args.length < 4) return;
        var services = Services.get();
        String sub = args[0].toLowerCase(Locale.ROOT);
        Crate crate = crateOrMsg(sender, args[1]);
        if (crate == null) return;
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            services.lang().send(sender, "player-not-found", Placeholder.unparsed("name", args[2]));
            return;
        }
        int amount = parseInt(args[3], 1);
        if (sub.equals("give")) {
            services.rerolls().give(target.getUniqueId(), crate, amount);
            services.lang().send(sender, "reroll-given",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("crate", crate.id()),
                    Placeholder.unparsed("player", target.getName()));
        } else if (sub.equals("take")) {
            services.rerolls().take(target.getUniqueId(), crate, amount);
            services.lang().send(sender, "reroll-taken",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("crate", crate.id()),
                    Placeholder.unparsed("player", target.getName()));
        }
    }

    private void resetCooldown(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.resetcooldown") || args.length < 2) return;
        var services = Services.get();
        Crate crate = crateOrMsg(sender, args[0]);
        if (crate == null) return;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            services.lang().send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        services.players().of(target).setCooldown(crate.id(), 0);
        services.lang().send(sender, "cooldown-reset",
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("crate", crate.id()));
    }

    private void resetWinLimit(CommandSender sender, String[] args) {
        if (denied(sender, "nocrates.command.resetcooldown") || args.length < 2) return;
        var services = Services.get();
        String scope = args[0].toLowerCase(Locale.ROOT);
        Crate crate = crateOrMsg(sender, args[1]);
        if (crate == null) return;
        if (scope.equals("global")) {
            services.winLimits().resetGlobal(crate.id());
            services.lang().send(sender, "winlimit-reset", Placeholder.unparsed("target", "global " + crate.id()));
            return;
        }
        if (args.length < 3) return;
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            services.lang().send(sender, "player-not-found", Placeholder.unparsed("name", args[2]));
            return;
        }
        for (Reward reward : crate.rewards().values()) {
            services.players().of(target).resetWins(crate.id(), reward.id());
        }
        services.lang().send(sender, "winlimit-reset",
                Placeholder.unparsed("target", target.getName() + " @ " + crate.id()));
    }

    private void stats(CommandSender sender, String[] args) {
        var services = Services.get();
        Player target = args.length >= 1 ? Bukkit.getPlayerExact(args[0])
                : sender instanceof Player self ? self : null;
        if (target == null) {
            services.lang().send(sender, "player-only");
            return;
        }
        services.lang().send(sender, "stats-header", Placeholder.unparsed("player", target.getName()));
        for (Crate crate : services.crates().all()) {
            int opens = services.players().of(target).opens(crate.id());
            if (opens == 0) continue;
            services.lang().sendRaw(sender, "stats-line",
                    Placeholder.unparsed("crate", crate.id()),
                    Placeholder.unparsed("opens", String.valueOf(opens)));
        }
    }

    // --- helpers ---

    private CratePlacement nearestPlacement(Crate crate, Player player) {
        CratePlacement best = null;
        double bestDist = Double.MAX_VALUE;
        for (CratePlacement placement : Services.get().placements().of(crate)) {
            Location loc = placement.location();
            if (loc == null || loc.getWorld() != player.getWorld()) continue;
            double dist = loc.distanceSquared(player.getLocation());
            if (dist < bestDist) {
                bestDist = dist;
                best = placement;
            }
        }
        return bestDist <= 64 * 64 ? best : null;
    }

    private Crate crateOrMsg(CommandSender sender, String id) {
        Crate crate = Services.get().crates().get(id);
        if (crate == null) {
            Services.get().lang().send(sender, "crate-not-found", Placeholder.unparsed("crate", id));
        }
        return crate;
    }

    private Key keyOrMsg(CommandSender sender, String id) {
        Key key = Services.get().keys().get(id);
        if (key == null) {
            Services.get().lang().send(sender, "key-not-found", Placeholder.unparsed("key", id));
        }
        return key;
    }

    private boolean denied(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("nocrates.admin")) return false;
        Services.get().lang().send(sender, "no-permission");
        return true;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // --- tab completion ---

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        var services = Services.get();
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> verbs = new ArrayList<>(List.of(
                    "help", "open", "preview", "virtualkeys", "list", "stats",
                    "create", "delete", "clone", "enable", "disable", "edit", "editor",
                    "givecrate", "placecrate", "attach", "detach",
                    "givereward", "giverandomreward", "key", "reroll",
                    "resetcooldown", "resetwinlimit", "reload"));
            verbs.addAll(EXTRA.keySet());
            return StringUtil.copyPartialMatches(args[0], verbs, out);
        }
        String verb = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (verb) {
                case "open", "preview", "delete", "clone", "enable", "disable", "edit", "givecrate",
                        "placecrate", "attach", "givereward", "giverandomreward", "resetcooldown", "massopen" ->
                        StringUtil.copyPartialMatches(args[1], services.crates().ids(), out);
                case "key" -> StringUtil.copyPartialMatches(args[1],
                        List.of("give", "giveall", "take", "set", "check", "pay"), out);
                case "reroll" -> StringUtil.copyPartialMatches(args[1], List.of("give", "take"), out);
                case "resetwinlimit" -> StringUtil.copyPartialMatches(args[1], List.of("player", "global"), out);
                default -> {
                }
            }
            return out;
        }
        if (args.length == 3) {
            switch (verb) {
                case "key" -> StringUtil.copyPartialMatches(args[2],
                        services.keys().all().stream().map(Key::id).toList(), out);
                case "reroll", "resetwinlimit" ->
                        StringUtil.copyPartialMatches(args[2], services.crates().ids(), out);
                case "givereward" -> {
                    Crate crate = services.crates().get(args[1]);
                    if (crate != null) {
                        StringUtil.copyPartialMatches(args[2], crate.rewards().keySet(), out);
                    }
                }
                default -> {
                }
            }
            return out;
        }
        return out;
    }
}
