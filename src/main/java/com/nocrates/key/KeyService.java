package com.nocrates.key;

import com.nocrates.storage.PlayerCache;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Key balances and consumption. Consumption over a crate's {@link KeyLink}s is
 * all-or-nothing: availability is planned first (virtual balance before physical items,
 * links in ascending priority order) and only executed when every link is satisfiable.
 */
public final class KeyService {

    private final KeyRegistry registry;
    private final PlayerCache players;

    public KeyService(KeyRegistry registry, PlayerCache players) {
        this.registry = registry;
        this.players = players;
    }

    // --- virtual balances ---

    public int balance(Player player, String keyId) {
        return players.of(player).keys(keyId);
    }

    public void give(UUID player, String keyId, int amount) {
        players.withOffline(player, data -> data.addKeys(keyId, Math.max(0, amount)));
    }

    public boolean take(Player player, String keyId, int amount) {
        var data = players.of(player);
        if (data.keys(keyId) < amount) return false;
        data.addKeys(keyId, -amount);
        return true;
    }

    public void set(UUID player, String keyId, int amount) {
        players.withOffline(player, data -> data.setKeys(keyId, Math.max(0, amount)));
    }

    public boolean pay(Player from, UUID to, String keyId, int amount) {
        if (from.getUniqueId().equals(to) || amount <= 0) return false;
        var data = players.of(from);
        if (data.keys(keyId) < amount) return false;
        data.addKeys(keyId, -amount);
        players.withOffline(to, target -> target.addKeys(keyId, amount));
        return true;
    }

    // --- physical keys ---

    public String keyIdOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(Key.PDC_KEY, PersistentDataType.STRING);
    }

    public int countPhysical(Player player, String keyId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && keyId.equals(keyIdOf(item))) count += item.getAmount();
        }
        return count;
    }

    private void removePhysical(Player player, String keyId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !keyId.equals(keyIdOf(item))) continue;
            int take = Math.min(remaining, item.getAmount());
            remaining -= take;
            if (take >= item.getAmount()) player.getInventory().setItem(i, null);
            else item.setAmount(item.getAmount() - take);
        }
    }

    // --- linked consumption ---

    /** Links in consumption order: ascending priority, ties by key id for determinism. */
    public static List<KeyLink> ordered(List<KeyLink> links) {
        List<KeyLink> out = new ArrayList<>(links);
        out.sort(Comparator.comparingInt(KeyLink::priority).thenComparing(KeyLink::keyId));
        return out;
    }

    /**
     * Pure planning step: {@code available} maps keyId -> [virtual, physical] counts.
     * Returns keyId -> [virtualToTake, physicalToTake] covering every link, or null when
     * any link cannot be satisfied (nothing may be consumed in that case).
     */
    public static Map<String, int[]> plan(Map<String, int[]> available, List<KeyLink> links) {
        Map<String, int[]> taken = new LinkedHashMap<>();
        Map<String, int[]> pool = new LinkedHashMap<>();
        available.forEach((k, v) -> pool.put(k, new int[]{v[0], v[1]}));
        for (KeyLink link : ordered(links)) {
            int[] have = pool.getOrDefault(link.keyId(), new int[]{0, 0});
            int need = link.amount();
            int fromVirtual = Math.min(need, have[0]);
            int fromPhysical = Math.min(need - fromVirtual, have[1]);
            if (fromVirtual + fromPhysical < need) return null;
            have[0] -= fromVirtual;
            have[1] -= fromPhysical;
            pool.put(link.keyId(), have);
            int[] agg = taken.computeIfAbsent(link.keyId(), k -> new int[]{0, 0});
            agg[0] += fromVirtual;
            agg[1] += fromPhysical;
        }
        return taken;
    }

    /** True when the player could satisfy every link right now. */
    public boolean has(Player player, List<KeyLink> links) {
        return plan(availability(player, links), links) != null;
    }

    /** All-or-nothing consumption of every link; false (and no change) when short. */
    public boolean consume(Player player, List<KeyLink> links) {
        Map<String, int[]> plan = plan(availability(player, links), links);
        if (plan == null) return false;
        var data = players.of(player);
        plan.forEach((keyId, take) -> {
            if (take[0] > 0) data.addKeys(keyId, -take[0]);
            if (take[1] > 0) removePhysical(player, keyId, take[1]);
        });
        return true;
    }

    private Map<String, int[]> availability(Player player, List<KeyLink> links) {
        Map<String, int[]> available = new LinkedHashMap<>();
        for (KeyLink link : links) {
            if (available.containsKey(link.keyId())) continue;
            Key key = registry.get(link.keyId());
            int virtual = players.of(player).keys(link.keyId());
            int physical = (key != null && key.virtual()) ? 0 : countPhysical(player, link.keyId());
            available.put(link.keyId(), new int[]{virtual, physical});
        }
        return available;
    }

    public KeyRegistry registry() {
        return registry;
    }
}
