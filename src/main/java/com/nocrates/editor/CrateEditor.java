package com.nocrates.editor;

import com.nocrates.NoCrates;
import com.nocrates.crate.Crate;
import com.nocrates.crate.CrateKeySpec;
import com.nocrates.gui.Menu;
import com.nocrates.key.KeyType;
import com.nocrates.reward.Pity;
import com.nocrates.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.function.Function;

/** Edits one crate's properties; every change is saved immediately. */
public final class CrateEditor extends Menu {

    private static final String[] ANIMATIONS =
            {"csgo", "reveal", "roulette", "cascade", "physical", "instant", "chesthunt"};
    private static final String[] KEY_TYPES = {"none", "virtual", "physical", "both"};

    private final NoCrates plugin;
    private final String crateName;

    public CrateEditor(NoCrates plugin, String crateName) {
        super(54, Items.mini("<dark_gray>Editing · " + crateName));
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

        set(10, Items.icon(Material.NAME_TAG, "<yellow>Display Name",
                "<gray>" + c.displayName(), " ", "<yellow>Click to change"),
                e -> prompt(player, in -> edit(b -> b.displayName(in))));

        set(12, Items.icon(Material.CLOCK, "<yellow>Animation",
                "<gray>Current: <white>" + c.animation(), " ", "<yellow>Left/Right click to cycle"),
                e -> apply(player, b -> b.animation(EditorUtil.cycle(ANIMATIONS, crate().animation(), e.isRightClick()))));

        set(14, Items.icon(Material.TRIPWIRE_HOOK, "<yellow>Key Type",
                "<gray>Current: <white>" + c.key().type().name().toLowerCase(Locale.ROOT),
                " ", "<yellow>Left/Right click to cycle"),
                e -> {
                    String next = EditorUtil.cycle(KEY_TYPES, crate().key().type().name().toLowerCase(Locale.ROOT), e.isRightClick());
                    CrateKeySpec key = new CrateKeySpec(KeyType.from(next), crate().key().keyId(), crate().key().item());
                    apply(player, b -> b.key(key));
                });

        set(16, Items.icon(c.broadcast() ? Material.LIME_DYE : Material.GRAY_DYE, "<yellow>Broadcast Wins",
                "<gray>Status: " + (c.broadcast() ? "<green>ON" : "<red>OFF"), " ", "<yellow>Click to toggle"),
                e -> apply(player, b -> b.broadcast(!crate().broadcast())));

        set(28, Items.icon(Material.REPEATER, "<yellow>Cooldown",
                "<gray>" + c.cooldownSeconds() + "s", " ", "<yellow>Click to set seconds"),
                e -> prompt(player, in -> edit(b -> b.cooldownSeconds(EditorUtil.parseInt(in, crate().cooldownSeconds())))));

        Pity pity = c.pity();
        set(30, Items.icon(Material.EXPERIENCE_BOTTLE, "<yellow>Pity / Milestone",
                pity.enabled() ? "<green>Every " + pity.every() + " → " + pity.tier() : "<red>OFF",
                " ", "<yellow>Left: set 'every'  ·  Right: toggle"),
                e -> {
                    if (e.isRightClick()) {
                        Pity p = crate().pity();
                        apply(player, b -> b.pity(new Pity(!p.enabled(),
                                p.every() <= 0 ? 25 : p.every(), p.tier() == null ? "legendary" : p.tier())));
                    } else {
                        prompt(player, in -> {
                            int every = EditorUtil.parseInt(in, 25);
                            String tier = crate().pity().tier() == null ? "legendary" : crate().pity().tier();
                            edit(b -> b.pity(new Pity(every > 0, every, tier)));
                        });
                    }
                });

        set(32, Items.icon(Material.CHEST, "<yellow>Rewards <gray>(" + c.rewards().size() + ")",
                " ", "<yellow>Click to edit rewards"),
                e -> new RewardEditor(plugin, crateName).open(player));

        set(34, Items.icon(Material.GRASS_BLOCK, "<yellow>Crate Block",
                "<gray>Look at a block and run", "<white>/crates setblock " + crateName), e -> {
        });

        set(48, Items.icon(Material.BARRIER, "<red>Delete Crate", "<gray>Shift-click to confirm"),
                e -> {
                    if (e.isShiftClick()) {
                        plugin.services().crates().remove(crateName);
                        plugin.services().crateBlocks().refresh();
                        new EditorHub(plugin).open(player);
                    }
                });

        set(49, Items.icon(Material.ARROW, "<gray>Back"), e -> new EditorHub(plugin).open(player));
        set(50, Items.icon(Material.EMERALD_BLOCK, "<green>Done"), e -> {
            plugin.services().crateBlocks().refresh();
            player.closeInventory();
            plugin.services().messages().send(player, "editor-saved");
        });
    }

    /** Apply an edit and refresh the open menu (for toggles/cycles). */
    private void apply(Player player, Function<Crate.Builder, Crate.Builder> change) {
        edit(change);
        refresh(player);
    }

    /** Apply an edit and persist. */
    private void edit(Function<Crate.Builder, Crate.Builder> change) {
        Crate current = crate();
        if (current != null) {
            plugin.services().crates().save(change.apply(Crate.builder(current)).build());
        }
    }

    private void prompt(Player player, java.util.function.Consumer<String> callback) {
        player.closeInventory();
        plugin.services().chatPrompts().await(player, in -> {
            callback.accept(in);
            open(player);
        });
    }
}
