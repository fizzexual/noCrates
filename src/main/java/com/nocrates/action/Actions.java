package com.nocrates.action;

import com.nocrates.compat.Compat;
import com.nocrates.compat.Scheduling;
import com.nocrates.hook.Hooks;
import com.nocrates.text.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * PhoenixCrates-style action lines: {@code [TYPE] args}. Used by menu icons, reward
 * follow-ups and modules. [DELAY] suspends the remaining chain. Unknown types are
 * reported by {@link #validate(List)} and skipped at run time with a single warning.
 */
public final class Actions {

    private final Plugin plugin;
    private final Map<String, ActionType> types = new HashMap<>();
    /** Late-bound openers so core actions can open menus/crates without hard deps. */
    private BiConsumer<Player, String> menuOpener;
    private BiConsumer<Player, String> crateOpener;

    public Actions(Plugin plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    public void register(ActionType type) {
        types.put(type.id().toUpperCase(Locale.ROOT), type);
    }

    public void menuOpener(BiConsumer<Player, String> opener) {
        this.menuOpener = opener;
    }

    public void crateOpener(BiConsumer<Player, String> opener) {
        this.crateOpener = opener;
    }

    /** "[TYPE] rest" -> {TYPE, rest}; null when the line has no [TYPE] prefix. */
    public static String[] split(String line) {
        if (line == null) return null;
        String s = line.trim();
        if (!s.startsWith("[")) return null;
        int close = s.indexOf(']');
        if (close <= 1) return null;
        String type = s.substring(1, close).trim().toUpperCase(Locale.ROOT);
        String args = s.substring(close + 1).trim();
        return new String[]{type, args};
    }

    /** Returns problems (one per bad line); empty list = all lines parseable. */
    public List<String> validate(List<String> lines) {
        List<String> problems = new ArrayList<>();
        if (lines == null) return problems;
        for (String line : lines) {
            String[] parts = split(line);
            if (parts == null) {
                problems.add("Not an action line: '" + line + "' (expected \"[TYPE] args\")");
            } else if (!types.containsKey(parts[0])) {
                problems.add("Unknown action [" + parts[0] + "] in '" + line + "'");
            }
        }
        return problems;
    }

    public void run(Player player, List<String> lines, Map<String, String> placeholders) {
        if (player == null || lines == null || lines.isEmpty()) return;
        runChain(new ActionContext(plugin, player, placeholders == null ? Map.of() : placeholders),
                new ArrayList<>(lines), 0);
    }

    private void runChain(ActionContext ctx, List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            String[] parts = split(lines.get(i));
            if (parts == null) continue;
            if (parts[0].equals("DELAY")) {
                long seconds = parseLong(parts[1], 1);
                int next = i + 1;
                if (next < lines.size() && ctx.player().isOnline()) {
                    Scheduling.later(ctx.plugin(), ctx.player().getLocation(), seconds * 20L,
                            () -> runChain(ctx, lines, next));
                }
                return;
            }
            ActionType type = types.get(parts[0]);
            if (type == null) {
                plugin.getLogger().warning("Skipping unknown action [" + parts[0] + "]");
                continue;
            }
            try {
                type.run(ctx, ctx.apply(parts[1]));
            } catch (Exception e) {
                plugin.getLogger().warning("Action [" + parts[0] + "] failed: " + e.getMessage());
            }
        }
    }

    private static long parseLong(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void registerDefaults() {
        register(new Simple("MESSAGE", (ctx, args) ->
                ctx.player().sendMessage(Text.mm(Hooks.papiApply(ctx.player(), args)))));

        register(new Simple("BROADCAST", (ctx, args) ->
                Bukkit.getServer().sendMessage(Text.mm(Hooks.papiApply(ctx.player(), args)))));

        register(new Simple("ACTION_BAR", (ctx, args) ->
                ctx.player().sendActionBar(Text.mm(Hooks.papiApply(ctx.player(), args)))));

        register(new Simple("TITLE", (ctx, args) -> {
            String[] p = args.split(";", -1);
            String title = p.length > 0 ? p[0] : "";
            String subtitle = p.length > 1 ? p[1] : "";
            int in = p.length > 2 ? parseInt(p[2], 10) : 10;
            int stay = p.length > 3 ? parseInt(p[3], 60) : 60;
            int out = p.length > 4 ? parseInt(p[4], 10) : 10;
            ctx.player().showTitle(Title.title(Text.mm(title), Text.mm(subtitle),
                    Title.Times.times(Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L))));
        }));

        register(new Simple("CLOSE_INVENTORY", (ctx, args) -> ctx.player().closeInventory()));

        register(new Simple("COMMAND", (ctx, args) -> {
            String target = "console";
            String command = args;
            int semi = args.indexOf(';');
            if (semi > 0) {
                String head = args.substring(0, semi).trim().toLowerCase(Locale.ROOT);
                if (head.equals("console") || head.equals("player")) {
                    target = head;
                    command = args.substring(semi + 1).trim();
                }
            }
            command = Hooks.papiApply(ctx.player(), command.replace("%player%", ctx.player().getName()));
            if (command.startsWith("/")) command = command.substring(1);
            if (target.equals("player")) {
                ctx.player().performCommand(command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }));

        register(new Simple("GAMEMODE", (ctx, args) -> {
            try {
                ctx.player().setGameMode(GameMode.valueOf(args.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }));

        register(new Simple("POTION_EFFECT", (ctx, args) -> {
            String[] p = args.split(";", -1);
            PotionEffectType type = potionEffect(p[0]);
            if (type == null) return;
            int duration = p.length > 1 ? parseInt(p[1], 5) : 5;
            int amplifier = p.length > 2 ? parseInt(p[2], 0) : 0;
            ctx.player().addPotionEffect(new PotionEffect(type, duration * 20, amplifier));
        }));

        register(new Simple("SOUND", (ctx, args) -> {
            String[] p = args.split(";", -1);
            float vol = p.length > 1 ? parseFloat(p[1], 1f) : 1f;
            float pitch = p.length > 2 ? parseFloat(p[2], 1f) : 1f;
            Compat.play(ctx.player(), p[0], vol, pitch);
        }));

        register(new Simple("MENU", (ctx, args) -> {
            if (menuOpener != null) menuOpener.accept(ctx.player(), args.trim());
        }));

        register(new Simple("OPEN", (ctx, args) -> {
            if (crateOpener != null) crateOpener.accept(ctx.player(), args.trim());
        }));

        // [DELAY] is handled inline by runChain but registered so validate() accepts it.
        register(new Simple("DELAY", (ctx, args) -> {
        }));
    }

    private static final String[][] POTION_RENAMES = {
            {"SLOW", "SLOWNESS"}, {"FAST_DIGGING", "HASTE"}, {"SLOW_DIGGING", "MINING_FATIGUE"},
            {"INCREASE_DAMAGE", "STRENGTH"}, {"HEAL", "INSTANT_HEALTH"}, {"HARM", "INSTANT_DAMAGE"},
            {"JUMP", "JUMP_BOOST"}, {"CONFUSION", "NAUSEA"}, {"DAMAGE_RESISTANCE", "RESISTANCE"}
    };

    @SuppressWarnings("deprecation")
    private static PotionEffectType potionEffect(String name) {
        String key = name.trim().toUpperCase(Locale.ROOT);
        PotionEffectType type = PotionEffectType.getByName(key);
        if (type != null) return type;
        for (String[] pair : POTION_RENAMES) {
            if (pair[0].equals(key)) type = PotionEffectType.getByName(pair[1]);
            else if (pair[1].equals(key)) type = PotionEffectType.getByName(pair[0]);
            if (type != null) return type;
        }
        return null;
    }

    private record Simple(String id, BiConsumer<ActionContext, String> fn) implements ActionType {
        @Override
        public void run(ActionContext ctx, String args) {
            fn.accept(ctx, args);
        }
    }
}
