package com.nocrates.storage;

import com.nocrates.NoCrates;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads {@link PlayerData} asynchronously on join, caches it in memory, saves on
 * quit, and flushes dirty entries on an interval. Other managers read state via
 * {@link #get(UUID)}.
 */
public final class PlayerDataManager implements Listener {

    private final NoCrates plugin;
    private final DataStore store;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private int taskId = -1;

    public PlayerDataManager(NoCrates plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            loadAsync(player.getUniqueId());
        }
        long ticks = Math.max(20L * 30, 20L * plugin.services().config().saveIntervalSeconds());
        this.taskId = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::flush, ticks, ticks).getTaskId();
    }

    private void loadAsync(UUID id) {
        store.load(id).thenAccept(data -> cache.put(id, data));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerData data = cache.remove(event.getPlayer().getUniqueId());
        if (data != null) {
            store.save(data);
        }
    }

    /** Cached data, loading synchronously as a fallback for offline access. */
    public PlayerData get(UUID id) {
        PlayerData data = cache.get(id);
        if (data == null) {
            data = store.load(id).join();
            cache.put(id, data);
        }
        return data;
    }

    public PlayerData getIfLoaded(UUID id) {
        return cache.get(id);
    }

    public void flush() {
        for (PlayerData data : cache.values()) {
            if (data.dirty()) {
                store.save(data);
            }
        }
    }

    public void shutdown() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        for (PlayerData data : cache.values()) {
            store.save(data);
        }
        cache.clear();
        store.close();
    }
}
