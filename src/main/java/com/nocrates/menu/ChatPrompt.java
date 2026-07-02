package com.nocrates.menu;

import com.nocrates.compat.Scheduling;
import com.nocrates.core.Services;
import com.nocrates.text.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One-shot chat input prompts used by the editor ("type the new value, or cancel").
 * The next chat message from a prompted player is captured instead of broadcast.
 */
public final class ChatPrompt implements Listener {

    private static final long TIMEOUT_TICKS = 20L * 60;

    private record Pending(Consumer<String> onInput, Runnable onCancel, long startedAt) {
    }

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** Closes the player's open menu and waits for one chat line. */
    public static void ask(Player player, String langKey, Consumer<String> onInput, Runnable onCancel) {
        Plugin plugin = Services.get().plugin();
        player.closeInventory();
        Services.get().lang().send(player, langKey);
        UUID id = player.getUniqueId();
        Pending pending = new Pending(onInput, onCancel, System.currentTimeMillis());
        PENDING.put(id, pending);
        Scheduling.later(plugin, null, TIMEOUT_TICKS, () -> {
            // Only expire this exact prompt; a newer prompt must not be cancelled by an old timer.
            if (PENDING.remove(id, pending)) {
                Services.get().lang().send(player, "editor-prompt-timeout");
                if (onCancel != null) Scheduling.entity(plugin, player, onCancel);
            }
        });
    }

    public static void ask(Player player, String langKey, Consumer<String> onInput) {
        ask(player, langKey, onInput, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Pending pending = PENDING.remove(event.getPlayer().getUniqueId());
        if (pending == null) return;
        event.setCancelled(true);
        String input = Text.plain(event.message()).trim();
        Player player = event.getPlayer();
        Plugin plugin = Services.get().plugin();
        Scheduling.entity(plugin, player, () -> {
            if (input.toLowerCase(Locale.ROOT).equals("cancel")) {
                Services.get().lang().send(player, "editor-prompt-cancelled");
                if (pending.onCancel != null) pending.onCancel.run();
                return;
            }
            pending.onInput.accept(input);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PENDING.remove(event.getPlayer().getUniqueId());
    }
}
