package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemSpec;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.Reward;
import com.nocrates.reward.WinLimit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Everything about one reward. */
public final class RewardEditor extends Menu {

    private final Crate crate;
    private final Reward reward;

    public RewardEditor(Player viewer, Crate crate, Reward reward) {
        super(viewer, "<dark_gray>Reward <dark_gray>» <white>" + reward.id(), 5);
        this.crate = crate;
        this.reward = reward;
    }

    private void save() {
        Services.get().crates().save(crate);
    }

    @Override
    protected void draw() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        set(13, new MenuItem(reward.displayItem().build()));

        set(10, new MenuItem(EditorIcons.button(Material.PAPER, "Weight: " + reward.percentage(),
                "Relative chance ("
                        + String.format(java.util.Locale.ROOT, "%.2f", crate.normalizedChance(reward)) + "% real).",
                "Click and type a number."), e -> promptNumber(n -> reward.percentage(n))));

        set(11, new MenuItem(EditorIcons.button(Material.ITEM_FRAME, "Display item from hand",
                "What previews/animations show."), e -> {
            clickSound();
            ItemStack hand = viewer.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                Services.get().lang().send(viewer, "editor-hold-item");
                return;
            }
            reward.displayItem(ItemSpec.fromItem(hand));
            save();
            refresh();
        }));

        set(12, new MenuItem(EditorIcons.button(Material.CHEST, "Add win-item from hand",
                "Won items: " + reward.winItems().size(),
                "Right-click clears them all."), e -> {
            clickSound();
            if (e.isRightClick()) {
                reward.winItems().clear();
                save();
                refresh();
                return;
            }
            ItemStack hand = viewer.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                Services.get().lang().send(viewer, "editor-hold-item");
                return;
            }
            reward.winItems().add(ItemSpec.fromItem(hand));
            save();
            refresh();
        }));

        set(14, new MenuItem(EditorIcons.button(Material.COMMAND_BLOCK,
                "Win commands (" + reward.winCommands().size() + ")",
                "Click: add one (type in chat,", "use %player%).",
                "Right-click: clear all."), e -> {
            clickSound();
            if (e.isRightClick()) {
                reward.winCommands().clear();
                save();
                refresh();
                return;
            }
            prompt(input -> reward.winCommands().add(input));
        }));

        set(15, new MenuItem(EditorIcons.toggle("Broadcast", reward.broadcast(),
                "Announce wins to the whole server."), e -> {
            clickSound();
            reward.broadcast(!reward.broadcast());
            save();
            refresh();
        }));

        set(16, new MenuItem(EditorIcons.toggle("Virtual reward", reward.virtualReward(),
                "Commands only, no physical items."), e -> {
            clickSound();
            reward.virtualReward(!reward.virtualReward());
            save();
            refresh();
        }));

        set(19, new MenuItem(EditorIcons.button(Material.NAME_TAG,
                "Rarity: " + (reward.rarity() == null ? "none" : reward.rarity()),
                "Click and type a rarity id", "from rarities.yml ('none' clears)."), e -> prompt(input -> {
            reward.rarity(input.equalsIgnoreCase("none") ? null : input.toLowerCase(java.util.Locale.ROOT));
        })));

        set(20, new MenuItem(EditorIcons.button(Material.PLAYER_HEAD,
                "Player win limit: " + reward.playerLimit().max(),
                "-1 = unlimited. Click and type."), e -> promptNumber(n ->
                reward.playerLimit(new WinLimit((int) n, reward.playerLimit().cooldownSeconds())))));

        set(21, new MenuItem(EditorIcons.button(Material.BEACON,
                "Global win limit: " + reward.globalLimit().max(),
                "-1 = unlimited. Across all players."), e -> promptNumber(n ->
                reward.globalLimit(new WinLimit((int) n, reward.globalLimit().cooldownSeconds())))));

        set(22, new MenuItem(EditorIcons.button(Material.TRIPWIRE_HOOK,
                "Selective cost: " + reward.selectiveCost(),
                "Key cost when the crate is in", "SELECTIVE (pick-your-reward) mode."), e ->
                promptNumber(n -> reward.selectiveCost((int) n))));

        set(23, new MenuItem(EditorIcons.toggle("Share online", reward.shareOnline(),
                "Everyone online gets the commands", "when this is won."), e -> {
            clickSound();
            reward.shareOnline(!reward.shareOnline());
            save();
            refresh();
        }));

        set(24, new MenuItem(EditorIcons.button(Material.BARRIER,
                "Restricted permissions (" + reward.restrictedPermissions().size() + ")",
                "Players WITH these perms can't win this.",
                "Click: add one. Right-click: clear.",
                "Configure the alternative reward in YAML."), e -> {
            clickSound();
            if (e.isRightClick()) {
                reward.restrictedPermissions().clear();
                save();
                refresh();
                return;
            }
            prompt(input -> reward.restrictedPermissions().add(input));
        }));

        set(36, new MenuItem(EditorIcons.back(), e -> new RewardListEditor(viewer, crate).open()));
    }

    private void prompt(java.util.function.Consumer<String> onInput) {
        clickSound();
        ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
            onInput.accept(input);
            save();
            Services.get().lang().send(viewer, "editor-saved");
            new RewardEditor(viewer, crate, reward).open();
        }, () -> new RewardEditor(viewer, crate, reward).open());
    }

    private void promptNumber(java.util.function.DoubleConsumer onInput) {
        clickSound();
        ChatPrompt.ask(viewer, "editor-prompt-number", input -> {
            try {
                onInput.accept(Double.parseDouble(input.trim()));
                save();
                Services.get().lang().send(viewer, "editor-saved");
            } catch (NumberFormatException e) {
                Services.get().lang().send(viewer, "not-a-number",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("input", input));
            }
            new RewardEditor(viewer, crate, reward).open();
        }, () -> new RewardEditor(viewer, crate, reward).open());
    }
}
