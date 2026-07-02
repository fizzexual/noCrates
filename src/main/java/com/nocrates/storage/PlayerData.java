package com.nocrates.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player crate data: virtual key balances, open counters, cooldowns, per-reward win
 * counts/cooldowns, guaranteed-win progress, reroll balances and stored claims.
 * Thread-safe via synchronization; mutations mark the object dirty for the flush task.
 */
public final class PlayerData {

    private final UUID id;
    private final Map<String, Integer> keys = new HashMap<>();
    private final Map<String, Integer> opens = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Integer> wins = new HashMap<>();
    private final Map<String, Long> winCooldowns = new HashMap<>();
    private final Map<String, Integer> milestones = new HashMap<>();
    private final Map<String, Integer> rerolls = new HashMap<>();
    private final List<String> claims = new ArrayList<>();
    private volatile boolean dirty;
    /** False for join placeholders until the async store load has been merged in. */
    private volatile boolean loaded;

    public PlayerData(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }

    public boolean dirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public boolean loaded() {
        return loaded;
    }

    public void markLoaded() {
        loaded = true;
    }

    /**
     * Merges another data object's values into this one: counters add, cooldown
     * timestamps take the max, claims append. Used to fold a join placeholder's
     * mutations into freshly loaded data, and quit-time placeholders into a fresh
     * load — so a placeholder can never overwrite stored data wholesale.
     */
    public synchronized void mergeFrom(PlayerData other) {
        for (String scope : new String[]{"keys", "opens", "wins", "rerolls"}) {
            other.rawInts(scope).forEach((k, v) -> {
                if (v != 0) loadIntAdd(scope, k, v);
            });
        }
        // milestone pointers move forward only
        other.rawInts("milestones").forEach((k, v) ->
                loadInt("milestones", k, Math.max(milestones.getOrDefault(k, 0), v)));
        other.rawLongs("cooldowns").forEach((k, v) ->
                cooldowns.merge(k, v, Math::max));
        other.rawLongs("win-cooldowns").forEach((k, v) ->
                winCooldowns.merge(k, v, Math::max));
        for (String claim : other.claims()) claims.add(claim);
        if (other.dirty) dirty = true;
    }

    private void loadIntAdd(String scope, String key, int delta) {
        Map<String, Integer> target = switch (scope) {
            case "keys" -> keys;
            case "opens" -> opens;
            case "wins" -> wins;
            case "rerolls" -> rerolls;
            default -> null;
        };
        if (target != null) target.merge(key, delta, Integer::sum);
    }

    private void touch() {
        dirty = true;
    }

    private static String winKey(String crate, String reward) {
        return crate + ":" + reward;
    }

    // --- keys ---

    public synchronized int keys(String keyId) {
        return keys.getOrDefault(keyId, 0);
    }

    public synchronized void addKeys(String keyId, int delta) {
        int now = Math.max(0, keys(keyId) + delta);
        if (now == 0) keys.remove(keyId);
        else keys.put(keyId, now);
        touch();
    }

    public synchronized void setKeys(String keyId, int amount) {
        if (amount <= 0) keys.remove(keyId);
        else keys.put(keyId, amount);
        touch();
    }

    public synchronized Map<String, Integer> keysView() {
        return new HashMap<>(keys);
    }

    // --- opens / cooldowns ---

    public synchronized int opens(String crateId) {
        return opens.getOrDefault(crateId, 0);
    }

    public synchronized int totalOpens() {
        return opens.values().stream().mapToInt(Integer::intValue).sum();
    }

    public synchronized void incrOpens(String crateId) {
        opens.merge(crateId, 1, Integer::sum);
        touch();
    }

    public synchronized Map<String, Integer> opensView() {
        return new HashMap<>(opens);
    }

    public synchronized long cooldownUntil(String crateId) {
        return cooldowns.getOrDefault(crateId, 0L);
    }

    public synchronized void setCooldown(String crateId, long untilEpochSec) {
        if (untilEpochSec <= 0) cooldowns.remove(crateId);
        else cooldowns.put(crateId, untilEpochSec);
        touch();
    }

    // --- per-reward wins ---

    public synchronized int wins(String crateId, String rewardId) {
        return wins.getOrDefault(winKey(crateId, rewardId), 0);
    }

    public synchronized void incrWins(String crateId, String rewardId) {
        wins.merge(winKey(crateId, rewardId), 1, Integer::sum);
        touch();
    }

    public synchronized void resetWins(String crateId, String rewardId) {
        wins.remove(winKey(crateId, rewardId));
        winCooldowns.remove(winKey(crateId, rewardId));
        touch();
    }

    public synchronized long winCooldownUntil(String crateId, String rewardId) {
        return winCooldowns.getOrDefault(winKey(crateId, rewardId), 0L);
    }

    public synchronized void setWinCooldown(String crateId, String rewardId, long untilEpochSec) {
        if (untilEpochSec <= 0) winCooldowns.remove(winKey(crateId, rewardId));
        else winCooldowns.put(winKey(crateId, rewardId), untilEpochSec);
        touch();
    }

    // --- guaranteed win / rerolls ---

    public synchronized int milestoneIndex(String crateId) {
        return milestones.getOrDefault(crateId, 0);
    }

    public synchronized void setMilestoneIndex(String crateId, int index) {
        milestones.put(crateId, index);
        touch();
    }

    public synchronized int rerolls(String crateId) {
        return rerolls.getOrDefault(crateId, 0);
    }

    public synchronized void addRerolls(String crateId, int delta) {
        int now = Math.max(0, rerolls(crateId) + delta);
        if (now == 0) rerolls.remove(crateId);
        else rerolls.put(crateId, now);
        touch();
    }

    // --- claims ---

    public synchronized List<String> claims() {
        return new ArrayList<>(claims);
    }

    public synchronized void addClaim(String row) {
        claims.add(row);
        touch();
    }

    public synchronized boolean removeClaim(String row) {
        boolean removed = claims.remove(row);
        if (removed) touch();
        return removed;
    }

    public synchronized void clearClaims() {
        claims.clear();
        touch();
    }

    // --- raw views for stores ---

    public synchronized Map<String, Integer> rawInts(String scope) {
        return switch (scope) {
            case "keys" -> new HashMap<>(keys);
            case "opens" -> new HashMap<>(opens);
            case "wins" -> new HashMap<>(wins);
            case "milestones" -> new HashMap<>(milestones);
            case "rerolls" -> new HashMap<>(rerolls);
            default -> new HashMap<>();
        };
    }

    public synchronized Map<String, Long> rawLongs(String scope) {
        return switch (scope) {
            case "cooldowns" -> new HashMap<>(cooldowns);
            case "win-cooldowns" -> new HashMap<>(winCooldowns);
            default -> new HashMap<>();
        };
    }

    public synchronized void loadInt(String scope, String key, int value) {
        Map<String, Integer> target = switch (scope) {
            case "keys" -> keys;
            case "opens" -> opens;
            case "wins" -> wins;
            case "milestones" -> milestones;
            case "rerolls" -> rerolls;
            default -> null;
        };
        if (target != null) target.put(key, value);
    }

    public synchronized void loadLong(String scope, String key, long value) {
        Map<String, Long> target = switch (scope) {
            case "cooldowns" -> cooldowns;
            case "win-cooldowns" -> winCooldowns;
            default -> null;
        };
        if (target != null) target.put(key, value);
    }

    public synchronized void loadClaim(String row) {
        claims.add(row);
    }
}
