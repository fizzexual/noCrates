package com.nocrates.reward;

import com.nocrates.compat.VersionCompat;
import com.nocrates.hook.Hooks;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Locale;
import java.util.Map;

/**
 * Parses reward action lines (a small {@code "prefix: args"} DSL) into
 * executable {@link RewardAction}s.
 *
 * <p>Supported prefixes: {@code item}, {@code command}/{@code console},
 * {@code playercommand}/{@code player}, {@code message}/{@code msg},
 * {@code broadcast}, {@code money}/{@code eco}, {@code xp}/{@code exp},
 * {@code permission}/{@code perm}, {@code sound}, {@code firework}.
 */
public final class RewardActions {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private RewardActions() {
    }

    public static RewardAction parse(String line) {
        if (line == null || line.isBlank()) {
            return player -> {
            };
        }
        String trimmed = line.trim();
        int idx = trimmed.indexOf(':');
        String type = (idx < 0 ? trimmed : trimmed.substring(0, idx)).trim().toLowerCase(Locale.ROOT);
        String arg = idx < 0 ? "" : trimmed.substring(idx + 1).trim();
        return switch (type) {
            case "item" -> new ItemAction(arg);
            case "command", "console" -> new CommandAction(arg, false);
            case "playercommand", "player" -> new CommandAction(arg, true);
            case "message", "msg" -> new MessageAction(arg);
            case "broadcast" -> new BroadcastAction(arg);
            case "money", "eco" -> new MoneyAction(parseDouble(arg, 0));
            case "xp", "exp" -> new XpAction((int) parseDouble(arg, 0));
            case "permission", "perm" -> new PermissionAction(arg);
            case "sound" -> SoundAction.parse(arg);
            case "firework" -> new FireworkAction();
            case "customitem", "custom" -> new CustomItemAction(arg);
            default -> player -> {
            };
        };
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String resolve(String text, Player player) {
        return text.replace("%player%", player.getName()).replace("%player_name%", player.getName());
    }

    // ---- action implementations ----------------------------------------

    private static final class ItemAction implements RewardAction {
        private final Material material;
        private final int amount;

        ItemAction(String arg) {
            String[] parts = arg.trim().split("\\s+");
            this.material = VersionCompat.material(parts.length > 0 ? parts[0] : "STONE", Material.STONE);
            int amt = 1;
            if (parts.length > 1) {
                try {
                    amt = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            this.amount = Math.max(1, amt);
        }

        @Override
        public void execute(Player player) {
            ItemStack item = new ItemStack(material, amount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack rem : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }
        }
    }

    private static final class CommandAction implements RewardAction {
        private final String command;
        private final boolean asPlayer;

        CommandAction(String command, boolean asPlayer) {
            this.command = command;
            this.asPlayer = asPlayer;
        }

        @Override
        public void execute(Player player) {
            String cmd = resolve(command, player);
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            if (asPlayer) {
                player.performCommand(cmd);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    private static final class MessageAction implements RewardAction {
        private final String text;

        MessageAction(String text) {
            this.text = text;
        }

        @Override
        public void execute(Player player) {
            player.sendMessage(MM.deserialize(resolve(text, player)));
        }
    }

    private static final class BroadcastAction implements RewardAction {
        private final String text;

        BroadcastAction(String text) {
            this.text = text;
        }

        @Override
        public void execute(Player player) {
            Bukkit.broadcast(MM.deserialize(resolve(text, player)));
        }
    }

    private static final class MoneyAction implements RewardAction {
        private final double amount;

        MoneyAction(double amount) {
            this.amount = amount;
        }

        @Override
        public void execute(Player player) {
            Hooks.economy().deposit(player, amount);
        }
    }

    private static final class XpAction implements RewardAction {
        private final int amount;

        XpAction(int amount) {
            this.amount = amount;
        }

        @Override
        public void execute(Player player) {
            if (amount > 0) {
                player.giveExp(amount);
            }
        }
    }

    private static final class PermissionAction implements RewardAction {
        private final String node;

        PermissionAction(String node) {
            this.node = node.trim();
        }

        @Override
        public void execute(Player player) {
            if (!node.isEmpty()) {
                Hooks.permissions().add(player, node);
            }
        }
    }

    private static final class SoundAction implements RewardAction {
        private final String key;
        private final float volume;
        private final float pitch;

        SoundAction(String key, float volume, float pitch) {
            this.key = key;
            this.volume = volume;
            this.pitch = pitch;
        }

        static SoundAction parse(String arg) {
            String[] parts = arg.trim().split("\\s+");
            String key = parts.length > 0 ? parts[0] : "";
            float volume = parts.length > 1 ? parseFloat(parts[1], 1f) : 1f;
            float pitch = parts.length > 2 ? parseFloat(parts[2], 1f) : 1f;
            return new SoundAction(key, volume, pitch);
        }

        @Override
        public void execute(Player player) {
            VersionCompat.playSound(player, key, volume, pitch);
        }
    }

    private static final class FireworkAction implements RewardAction {
        @Override
        public void execute(Player player) {
            try {
                Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.FUCHSIA, Color.AQUA)
                        .withFade(Color.WHITE)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .flicker(true)
                        .trail(true)
                        .build());
                meta.setPower(0);
                firework.setFireworkMeta(meta);
                firework.detonate();
            } catch (Throwable ignored) {
                // fireworks are cosmetic — never fail an open over them
            }
        }
    }

    /**
     * Gives an item from a custom-item plugin, resolved reflectively so the
     * plugin compiles and runs without those plugins present. Format:
     * {@code customitem: <provider> <id>} (MMOItems: {@code <type> <id>}).
     * Providers: itemsadder/ia, oraxen, nexo, mmoitems.
     */
    private static final class CustomItemAction implements RewardAction {
        private final String spec;

        CustomItemAction(String spec) {
            this.spec = spec;
        }

        @Override
        public void execute(Player player) {
            ItemStack item = resolve(spec);
            if (item != null) {
                player.getInventory().addItem(item).values()
                        .forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
            }
        }

        private static ItemStack resolve(String spec) {
            String[] parts = spec.trim().split("\\s+");
            if (parts.length < 2) {
                return null;
            }
            try {
                switch (parts[0].toLowerCase(Locale.ROOT)) {
                    case "itemsadder", "ia" -> {
                        Class<?> cls = Class.forName("dev.lone.itemsadder.api.CustomStack");
                        Object stack = cls.getMethod("getInstance", String.class).invoke(null, parts[1]);
                        return stack == null ? null
                                : (ItemStack) stack.getClass().getMethod("getItemStack").invoke(stack);
                    }
                    case "oraxen" -> {
                        Class<?> cls = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                        Object builder = cls.getMethod("getItemById", String.class).invoke(null, parts[1]);
                        return builder == null ? null
                                : (ItemStack) builder.getClass().getMethod("build").invoke(builder);
                    }
                    case "nexo" -> {
                        Class<?> cls = Class.forName("com.nexomc.nexo.api.NexoItems");
                        Object builder = cls.getMethod("itemFromId", String.class).invoke(null, parts[1]);
                        return builder == null ? null
                                : (ItemStack) builder.getClass().getMethod("build").invoke(builder);
                    }
                    case "mmoitems" -> {
                        if (parts.length < 3) {
                            return null;
                        }
                        Class<?> cls = Class.forName("net.Indyuce.mmoitems.MMOItems");
                        Object mmo = cls.getField("plugin").get(null);
                        return (ItemStack) mmo.getClass()
                                .getMethod("getItem", String.class, String.class)
                                .invoke(mmo, parts[1], parts[2]);
                    }
                    default -> {
                        return null;
                    }
                }
            } catch (Throwable t) {
                return null;
            }
        }
    }
}
