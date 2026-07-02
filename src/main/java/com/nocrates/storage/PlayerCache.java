package com.nocrates.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-memory cache in front of the {@link DataStore}.
 *
 * Safety rules (each closes a real data-loss hole):
 * <ul>
 *   <li>Entries start as UNLOADED placeholders; a placeholder is never saved wholesale —
 *       its mutations are merged as deltas into freshly loaded store data.</li>
 *   <li>Only LOADED entries are flushed/saved directly.</li>
 *   <li>Entries for players who left (late grants) are merged-and-evicted by the
 *       periodic flush instead of lingering and overwriting the quit save.</li>
 * </ul>
 */
public final class PlayerCache implements Listener {

    private final DataStore store;
    private final Plugin plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerCache(Plugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public PlayerData cached(UUID id) {
        return cache.get(id);
    }

    /** Cached data for a player; an empty unloaded placeholder until the async load lands. */
    public PlayerData of(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public CompletableFuture<PlayerData> get(UUID id) {
        PlayerData cached = cache.get(id);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return store.load(id);
    }

    /** Async load -> mutate -> save for players who may be offline. Aborts on load failure. */
    public void withOffline(UUID id, Consumer<PlayerData> mutation) {
        PlayerData cached = cache.get(id);
        if (cached != null) {
            mutation.accept(cached);
            return;
        }
        store.load(id).thenAccept(data -> {
            mutation.accept(data);
            store.saveAsync(data);
        }).exceptionally(err -> {
            plugin.getLogger().warning("Skipped offline data change for " + id
                    + " — load failed: " + err.getMessage());
            return null;
        });
    }

    /** Periodic flush: saves loaded dirty entries and merge-evicts entries for gone players. */
    public void flushDirty() {
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            UUID id = entry.getKey();
            PlayerData data = entry.getValue();
            boolean online = Bukkit.getPlayer(id) != null;
            if (online) {
                if (data.loaded() && data.dirty()) store.saveAsync(data);
                continue;
            }
            // player left; late grants may have created/modified this entry
            cache.remove(id, data);
            persistDeparted(id, data);
        }
    }

    public void flushAllSync() {
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            PlayerData data = entry.getValue();
            if (data.loaded()) {
                store.saveSync(data);
            } else if (data.dirty()) {
                // best effort at shutdown: merge the placeholder into stored data
                try {
                    PlayerData fresh = store.load(entry.getKey()).join();
                    fresh.mergeFrom(data);
                    store.saveSync(fresh);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not merge unloaded data for "
                            + entry.getKey() + ": " + e.getMessage());
                }
            }
        }
        cache.clear();
    }

    private void persistDeparted(UUID id, PlayerData data) {
        if (data.loaded()) {
            if (data.dirty()) store.saveAsync(data);
            return;
        }
        if (!data.dirty()) return;
        // unloaded placeholder with changes: merge into stored state, never overwrite
        store.load(id).thenAccept(fresh -> {
            fresh.mergeFrom(data);
            store.saveAsync(fresh);
        }).exceptionally(err -> {
            plugin.getLogger().warning("Could not persist late changes for " + id
                    + ": " + err.getMessage());
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        cache.computeIfAbsent(id, PlayerData::new);
        store.load(id).thenAccept(loaded -> {
            PlayerData placeholder = cache.get(id);
            if (placeholder != null && !placeholder.loaded()) {
                // fold everything done between join and load-completion into the real data
                loaded.mergeFrom(placeholder);
            }
            loaded.markLoaded();
            cache.put(id, loaded);
        }).exceptionally(err -> {
            plugin.getLogger().severe("Could not load data for " + event.getPlayer().getName()
                    + ": " + err.getMessage() + " — changes this session will be merged on quit.");
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerData data = cache.remove(id);
        if (data != null) persistDeparted(id, data);
    }
}
