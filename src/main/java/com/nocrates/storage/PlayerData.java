package com.nocrates.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player persistent state: virtual key balances, per-crate open counts and
 * per-reward win counts. Pure data (no Bukkit types) so the maths is unit
 * tested directly. Composite map keys are {@code crate:identifier}, lower-cased.
 */
public final class PlayerData {

    private final UUID uuid;
    private final Map<String, Integer> keys = new HashMap<>();
    private final Map<String, Integer> opens = new HashMap<>();
    private final Map<String, Integer> wins = new HashMap<>();
    private boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    private static String composite(String a, String b) {
        return a.toLowerCase() + ":" + b.toLowerCase();
    }

    public int keys(String crate, String keyId) {
        return keys.getOrDefault(composite(crate, keyId), 0);
    }

    public void addKeys(String crate, String keyId, int amount) {
        if (amount == 0) {
            return;
        }
        String key = composite(crate, keyId);
        int total = keys.getOrDefault(key, 0) + amount;
        if (total <= 0) {
            keys.remove(key);
        } else {
            keys.put(key, total);
        }
        dirty = true;
    }

    /** Remove keys if the player has enough; otherwise leave unchanged. */
    public boolean takeKeys(String crate, String keyId, int amount) {
        if (amount <= 0) {
            return true;
        }
        String key = composite(crate, keyId);
        int have = keys.getOrDefault(key, 0);
        if (have < amount) {
            return false;
        }
        int left = have - amount;
        if (left <= 0) {
            keys.remove(key);
        } else {
            keys.put(key, left);
        }
        dirty = true;
        return true;
    }

    public int opens(String crate) {
        return opens.getOrDefault(crate.toLowerCase(), 0);
    }

    public void incrOpens(String crate) {
        opens.merge(crate.toLowerCase(), 1, Integer::sum);
        dirty = true;
    }

    public int winCount(String crate, String rewardId) {
        return wins.getOrDefault(composite(crate, rewardId), 0);
    }

    public void incrWin(String crate, String rewardId) {
        wins.merge(composite(crate, rewardId), 1, Integer::sum);
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public Map<String, Integer> rawKeys() {
        return keys;
    }

    public Map<String, Integer> rawOpens() {
        return opens;
    }

    public Map<String, Integer> rawWins() {
        return wins;
    }
}
