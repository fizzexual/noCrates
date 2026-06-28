package com.nocrates.editor;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.gui.Menu;
import com.nocrates.reward.DisplaySpec;
import com.nocrates.reward.Reward;
import com.nocrates.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lists and edits a crate's rewards: add-from-hand, set chance, cycle rarity, remove. */
public final class RewardEditor extends Menu {

    private static final String[] RARITIES = {"common", "uncommon", "rare", "epic", "legendary", "mythic"};

    private final NoCrates plugin;
    private final String crateName;

    public RewardEditor(NoCrates plugin, String crateName) {
        super(54, Items.mini("<dark_gray>Rewards · " + crateName));
        this.plugin = plugin;
        this.crateName = crateName.toLowerCase(Locale.ROOT);
    }

    private Crate crate() {
        return plugin.services().crates().get(crateName);
    }

    @Override
    protected void build(Player player) {
        Crate c = crate();
        if (c == null) {
            new EditorHub(plugin).open(player);
            return;
        }
        int slot = 0;
        for (Reward reward : c.rewards()) {
            if (slot >= 45) {
                break;
            }
            set(slot++, icon(reward), event -> {
                if (event.isRightClick()) {
                    saveRewards(removeReward(crate(), reward.id()));
                    refresh(player);
                } else if (event.isShiftClick()) {
                    String next = EditorUtil.cycle(RARITIES, reward.rarityId(), false);
                    saveRewards(replaceReward(crate(), reward.id(), withRarity(reward, next)));
                    refresh(player);
                } else {
                    player.closeInventory();
                    plugin.services().chatPrompts().await(player, in -> {
                        double chance = EditorUtil.parseDouble(in, reward.weight());
                        saveRewards(replaceReward(crate(), reward.id(), withChance(reward, chance)));
                        open(player);
                    });
                }
            });
        }
        set(48, Items.icon(Material.HOPPER, "<green><bold>Add Reward From Hand",
                "<gray>Hold an item and click to add it", "<gray>as a reward (gives that item)"),
                event -> addFromHand(player));
        set(49, Items.icon(Material.ARROW, "<gray>Back"),
                event -> new CrateEditor(plugin, crateName).open(player));
    }

    private ItemStack icon(Reward reward) {
        ItemStack item = reward.display() != null
                ? Items.build(reward.display())
                : Items.icon(Material.PAPER, "<white>" + reward.id());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Items.mini("<dark_gray>id: " + reward.id()));
            lore.add(Items.mini("<gray>Rarity: <white>" + reward.rarityId()));
            lore.add(Items.mini("<gray>Chance (weight): <white>" + reward.weight()));
            lore.add(Items.mini(" "));
            lore.add(Items.mini("<yellow>Left-click: set chance"));
            lore.add(Items.mini("<yellow>Shift-click: cycle rarity"));
            lore.add(Items.mini("<red>Right-click: remove"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addFromHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            plugin.services().messages().send(player, "editor-no-hand");
            return;
        }
        Crate c = crate();
        if (c == null) {
            return;
        }
        DisplaySpec display = Items.fromItem(hand);
        String id = uniqueId(c, hand.getType().name().toLowerCase(Locale.ROOT));
        Reward reward = new Reward(id, "common", 10.0, display,
                List.of("item: " + hand.getType().name() + " " + hand.getAmount()), -1, 0);
        List<Reward> rewards = new ArrayList<>(c.rewards());
        rewards.add(reward);
        saveRewards(rewards);
        plugin.services().messages().send(player, "editor-reward-added");
        refresh(player);
    }

    private String uniqueId(Crate crate, String base) {
        String id = base;
        int counter = 1;
        while (rewardExists(crate, id)) {
            id = base + "_" + (++counter);
        }
        return id;
    }

    private boolean rewardExists(Crate crate, String id) {
        for (Reward reward : crate.rewards()) {
            if (reward.id().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private void saveRewards(List<Reward> rewards) {
        Crate c = crate();
        if (c != null) {
            plugin.services().crates().save(Crate.builder(c).rewards(rewards).build());
        }
    }

    private List<Reward> removeReward(Crate crate, String id) {
        List<Reward> list = new ArrayList<>();
        for (Reward reward : crate.rewards()) {
            if (!reward.id().equalsIgnoreCase(id)) {
                list.add(reward);
            }
        }
        return list;
    }

    private List<Reward> replaceReward(Crate crate, String id, Reward replacement) {
        List<Reward> list = new ArrayList<>();
        for (Reward reward : crate.rewards()) {
            list.add(reward.id().equalsIgnoreCase(id) ? replacement : reward);
        }
        return list;
    }

    private Reward withChance(Reward reward, double chance) {
        return new Reward(reward.id(), reward.rarityId(), chance, reward.display(),
                reward.actions(), reward.maxPerPlayer(), reward.cooldownSeconds());
    }

    private Reward withRarity(Reward reward, String rarity) {
        return new Reward(reward.id(), rarity, reward.weight(), reward.display(),
                reward.actions(), reward.maxPerPlayer(), reward.cooldownSeconds());
    }
}
