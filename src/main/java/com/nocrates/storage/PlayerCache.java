package com.nocrates.storage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-memory cache in front of the {@link DataStore}: loaded on join, saved and evicted
 * on quit, periodically flushed while dirty. Offline players are load-modify-saved
 * off-thread through {@link #withOffline}.
 */
public final class PlayerCache implements Listener {

    private final DataStore store;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerCache(DataStore store) {
        this.store = store;
    }

    public PlayerData cached(UUID id) {
        return cache.get(id);
    }

    /** Cached data for an online player; empty placeholder until the async load lands. */
    public PlayerData of(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public CompletableFuture<PlayerData> get(UUID id) {
        PlayerData cached = cache.get(id);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return store.load(id);
    }

    /** Async load -> mutate -> save for players who may be offline. */
    public void withOffline(UUID id, Consumer<PlayerData> mutation) {
        PlayerData cached = cache.get(id);
        if (cached != null) {
            mutation.accept(cached);
            return;
        }
        store.load(id).thenAccept(data -> {
            mutation.accept(data);
            store.saveAsync(data);
        });
    }

    public void flushDirty() {
        for (PlayerData data : cache.values()) {
            if (data.dirty()) store.saveAsync(data);
        }
    }

    public void flushAllSync() {
        for (PlayerData data : cache.values()) store.saveSync(data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        store.load(id).thenAccept(loaded -> {
            PlayerData placeholder = cache.put(id, loaded);
            if (placeholder != null && placeholder.dirty()) {
                // Something granted keys between join and load; merge the balances over.
                placeholder.keysView().forEach(loaded::addKeys);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PlayerData data = cache.remove(event.getPlayer().getUniqueId());
        if (data != null) store.saveAsync(data);
    }
}
