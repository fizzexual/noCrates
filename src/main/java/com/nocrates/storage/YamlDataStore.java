package com.nocrates.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Zero-setup flat-file backend: playerdata/&lt;uuid&gt;.yml + globals.yml. */
public final class YamlDataStore implements DataStore {

    private static final String[] INT_SCOPES = {"keys", "opens", "wins", "milestones", "rerolls"};
    private static final String[] LONG_SCOPES = {"cooldowns", "win-cooldowns"};

    private final Plugin plugin;
    private final File playerDir;
    private final File globalsFile;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "noCrates-io");
        t.setDaemon(true);
        return t;
    });
    private final Object globalsLock = new Object();
    private YamlConfiguration globals;

    public YamlDataStore(Plugin plugin) {
        this.plugin = plugin;
        this.playerDir = new File(plugin.getDataFolder(), "playerdata");
        this.globalsFile = new File(plugin.getDataFolder(), "globals.yml");
        playerDir.mkdirs();
        this.globals = YamlConfiguration.loadConfiguration(globalsFile);
    }

    @Override
    public CompletableFuture<PlayerData> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = new PlayerData(id);
            File file = new File(playerDir, id + ".yml");
            if (!file.isFile()) return data;
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            for (String scope : INT_SCOPES) {
                ConfigurationSection s = yml.getConfigurationSection(scope);
                if (s != null) for (String k : s.getKeys(false)) data.loadInt(scope, k, s.getInt(k));
            }
            for (String scope : LONG_SCOPES) {
                ConfigurationSection s = yml.getConfigurationSection(scope);
                if (s != null) for (String k : s.getKeys(false)) data.loadLong(scope, k, s.getLong(k));
            }
            for (String row : yml.getStringList("claims")) data.loadClaim(row);
            return data;
        }, io);
    }

    @Override
    public void saveAsync(PlayerData data) {
        io.submit(() -> writePlayer(data));
    }

    @Override
    public void saveSync(PlayerData data) {
        writePlayer(data);
    }

    private void writePlayer(PlayerData data) {
        YamlConfiguration yml = new YamlConfiguration();
        for (String scope : INT_SCOPES) {
            for (Map.Entry<String, Integer> e : data.rawInts(scope).entrySet()) {
                yml.set(scope + "." + e.getKey(), e.getValue());
            }
        }
        for (String scope : LONG_SCOPES) {
            for (Map.Entry<String, Long> e : data.rawLongs(scope).entrySet()) {
                yml.set(scope + "." + e.getKey(), e.getValue());
            }
        }
        List<String> claims = data.claims();
        if (!claims.isEmpty()) yml.set("claims", claims);
        try {
            yml.save(new File(playerDir, data.id() + ".yml"));
            data.clearDirty();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save player data " + data.id() + ": " + e.getMessage());
        }
    }

    // --- globals ---

    @Override
    public CompletableFuture<Map<String, Integer>> globalWins(String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> out = new HashMap<>();
            synchronized (globalsLock) {
                ConfigurationSection s = globals.getConfigurationSection("global-wins." + crateId);
                if (s != null) for (String k : s.getKeys(false)) out.put(k, s.getInt(k + ".count", s.getInt(k)));
            }
            return out;
        }, io);
    }

    @Override
    public CompletableFuture<Map<String, Long>> globalWinCooldowns(String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> out = new HashMap<>();
            synchronized (globalsLock) {
                ConfigurationSection s = globals.getConfigurationSection("global-wins." + crateId);
                if (s != null) for (String k : s.getKeys(false)) {
                    long until = s.getLong(k + ".cooldown-until", 0);
                    if (until > 0) out.put(k, until);
                }
            }
            return out;
        }, io);
    }

    @Override
    public void setGlobalWin(String crateId, String rewardId, int count, long cooldownUntilEpochSec) {
        io.submit(() -> {
            synchronized (globalsLock) {
                globals.set("global-wins." + crateId + "." + rewardId + ".count", count);
                globals.set("global-wins." + crateId + "." + rewardId + ".cooldown-until",
                        cooldownUntilEpochSec > 0 ? cooldownUntilEpochSec : null);
                saveGlobals();
            }
        });
    }

    @Override
    public void resetGlobalWins(String crateId) {
        io.submit(() -> {
            synchronized (globalsLock) {
                globals.set("global-wins." + crateId, null);
                saveGlobals();
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> lastWinners(String crateId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (globalsLock) {
                List<String> rows = globals.getStringList("last-winners." + crateId);
                return new ArrayList<>(rows.subList(0, Math.min(limit, rows.size())));
            }
        }, io);
    }

    @Override
    public void pushWinner(String crateId, String row, int keep) {
        io.submit(() -> {
            synchronized (globalsLock) {
                List<String> rows = new ArrayList<>(globals.getStringList("last-winners." + crateId));
                rows.add(0, row);
                while (rows.size() > Math.max(1, keep)) rows.remove(rows.size() - 1);
                globals.set("last-winners." + crateId, rows);
                saveGlobals();
            }
        });
    }

    private void saveGlobals() {
        try {
            globals.save(globalsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save globals.yml: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        io.shutdown();
        try {
            io.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
