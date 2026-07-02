package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.crate.RewardsMode;
import com.nocrates.item.ItemSpec;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** The per-crate control panel; every change saves to the crate file immediately. */
public final class CrateEditor extends Menu {

    private final Crate crate;

    public CrateEditor(Player viewer, Crate crate) {
        super(viewer, "<dark_gray>Edit <dark_gray>» <white>" + crate.id(), 6);
        this.crate = crate;
    }

    private void save() {
        Services.get().crates().save(crate);
        Services.get().placements().refresh(crate);
    }

    @Override
    protected void draw() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        set(10, new MenuItem(EditorIcons.button(Material.NAME_TAG, "Display name",
                "Currently: " + crate.displayName(), "", "Click, then type the new name."), e -> prompt(input -> {
            crate.displayName(input);
            save();
        })));

        set(11, new MenuItem(EditorIcons.button(Material.GRASS_BLOCK,
                        "Engine: " + crate.engine().name(),
                        "BLOCK = a real block (chest lid animates)",
                        "MODEL = floating item display",
                        "", "Click to switch. Right-click: set block", "material by typing its name."),
                e -> {
                    clickSound();
                    if (e.isRightClick()) {
                        prompt(input -> {
                            if (com.nocrates.compat.Compat.material(input, null) == null) {
                                Services.get().lang().send(viewer, "editor-invalid");
                            } else {
                                crate.blockMaterial(input.toUpperCase(java.util.Locale.ROOT));
                                save();
                            }
                        });
                        return;
                    }
                    crate.engine(crate.engine() == Crate.EngineType.BLOCK
                            ? Crate.EngineType.MODEL : Crate.EngineType.BLOCK);
                    save();
                    refresh();
                }));

        set(12, new MenuItem(EditorIcons.button(Material.ITEM_FRAME, "Model item from hand",
                "MODEL engine: use the item you are", "holding as the crate model."), e -> {
            clickSound();
            var hand = viewer.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                Services.get().lang().send(viewer, "editor-hold-item");
                return;
            }
            crate.modelItem(ItemSpec.fromItem(hand));
            save();
            Services.get().lang().send(viewer, "editor-saved");
        }));

        set(13, new MenuItem(EditorIcons.button(Material.CHEST, "Rewards (" + crate.rewards().size() + ")",
                "Add, edit, remove rewards."), e -> {
            clickSound();
            new RewardListEditor(viewer, crate).open();
        }));

        set(14, new MenuItem(EditorIcons.button(Material.TRIPWIRE_HOOK, "Key links (" + crate.keys().size() + ")",
                "Which keys this crate needs."), e -> {
            clickSound();
            new KeyLinksEditor(viewer, crate).open();
        }));

        set(15, new MenuItem(EditorIcons.button(Material.BLAZE_POWDER, "Animations",
                "Pre: " + crate.animation().preOpen(),
                "Post: " + crate.animation().postOpen(),
                "Display: " + crate.animation().rewardDisplay()), e -> {
            clickSound();
            new AnimationPicker(viewer, crate).open();
        }));

        set(16, new MenuItem(EditorIcons.button(Material.SPYGLASS, "Idle effects ("
                + crate.animation().idleEffects().size() + ")",
                "Always-on particle shapes."), e -> {
            clickSound();
            new IdleEffectEditor(viewer, crate).open();
        }));

        set(19, new MenuItem(EditorIcons.button(Material.GOLDEN_APPLE, "Guaranteed win",
                (crate.guaranteedEnabled() ? "<green>enabled" : "<red>disabled") + " <gray>("
                        + crate.milestones().size() + " milestones)"), e -> {
            clickSound();
            new MilestoneEditor(viewer, crate).open();
        }));

        set(20, new MenuItem(EditorIcons.toggle("Rerolls", crate.rerollEnabled(),
                "Free per open: " + crate.rerollFree(),
                "Right-click to set free rerolls."), e -> {
            clickSound();
            if (e.isRightClick()) {
                promptNumber(n -> {
                    crate.rerollFree((int) n);
                    save();
                });
                return;
            }
            crate.rerollEnabled(!crate.rerollEnabled());
            save();
            refresh();
        }));

        set(21, new MenuItem(EditorIcons.button(Material.GOLD_INGOT, "Open cost: " + crate.open().cost(),
                "Vault money charged per open.", "Click and type a number."), e -> promptNumber(n -> {
            crate.open().cost(n);
            save();
        })));

        set(22, new MenuItem(EditorIcons.button(Material.CLOCK, "Cooldown: " + crate.open().cooldownSeconds() + "s",
                "Seconds between opens per player."), e -> promptNumber(n -> {
            crate.open().cooldownSeconds((int) n);
            save();
        })));

        set(23, new MenuItem(EditorIcons.button(Material.COMPARATOR,
                "Rewards mode: " + crate.rewardsMode().name(),
                "RANDOM = weighted roll",
                "SELECTIVE = players pick their reward"), e -> {
            clickSound();
            crate.rewardsMode(crate.rewardsMode() == RewardsMode.RANDOM
                    ? RewardsMode.SELECTIVE : RewardsMode.RANDOM);
            save();
            refresh();
        }));

        set(24, new MenuItem(EditorIcons.button(Material.BUNDLE,
                "Max win rewards: " + crate.maxWinRewards(),
                "Rewards granted per opening (1-30)."), e -> promptNumber(n -> {
            crate.maxWinRewards((int) n);
            save();
        })));

        set(25, new MenuItem(EditorIcons.toggle("Quick open", crate.open().quickOpen(),
                "Sneak + right-click skips the animation."), e -> {
            clickSound();
            crate.open().quickOpen(!crate.open().quickOpen());
            save();
            refresh();
        }));

        set(28, new MenuItem(EditorIcons.toggle("Simultaneous opens", crate.open().simultaneous(),
                "Several players at once."), e -> {
            clickSound();
            crate.open().simultaneous(!crate.open().simultaneous());
            save();
            refresh();
        }));

        set(29, new MenuItem(EditorIcons.toggle("Knockback", crate.open().knockback(),
                "Push players without a key away."), e -> {
            clickSound();
            crate.open().knockback(!crate.open().knockback());
            save();
            refresh();
        }));

        set(30, new MenuItem(EditorIcons.toggle("Permission required", crate.permissionRequired(),
                "Requires " + crate.permission()), e -> {
            clickSound();
            crate.permissionRequired(!crate.permissionRequired());
            save();
            refresh();
        }));

        set(31, new MenuItem(EditorIcons.toggle("Preview", crate.previewEnabled(),
                "Left-click preview menu."), e -> {
            clickSound();
            crate.previewEnabled(!crate.previewEnabled());
            save();
            refresh();
        }));

        set(32, new MenuItem(EditorIcons.button(Material.OAK_SIGN, "Hologram lines ("
                        + crate.hologramLines().size() + ")",
                "Click: add a line (type in chat).",
                "Right-click: clear all lines."), e -> {
            clickSound();
            if (e.isRightClick()) {
                crate.hologramLines(List.of());
                save();
                refresh();
                return;
            }
            prompt(input -> {
                var lines = new java.util.ArrayList<>(crate.hologramLines());
                lines.add(input);
                crate.hologramLines(lines);
                save();
            });
        }));

        set(33, new MenuItem(EditorIcons.button(Material.ENDER_EYE, "Attach to target block",
                "Binds this crate to the block", "you are looking at."), e -> {
            clickSound();
            viewer.closeInventory();
            viewer.performCommand("crates attach " + crate.id());
        }));

        set(34, new MenuItem(EditorIcons.toggle("Crate enabled", crate.enabled()), e -> {
            clickSound();
            crate.enabled(!crate.enabled());
            save();
            refresh();
        }));

        set(40, new MenuItem(EditorIcons.button(Material.ENDER_PEARL, "Preview crate",
                "Opens the player preview."), e -> {
            clickSound();
            new com.nocrates.open.PreviewMenu(viewer, crate).open();
        }));

        set(45, new MenuItem(EditorIcons.back(), e -> new CrateListEditor(viewer).open()));

        set(53, new MenuItem(EditorIcons.button(Material.LAVA_BUCKET, "Delete crate",
                "Type the crate id in chat to confirm."), e -> prompt(input -> {
            if (input.equalsIgnoreCase(crate.id())) {
                Services.get().crates().delete(crate.id());
                Services.get().placements().rebuild();
                Services.get().lang().send(viewer, "crate-deleted",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("crate", crate.id()));
                new CrateListEditor(viewer).open();
                return;
            }
            Services.get().lang().send(viewer, "editor-prompt-cancelled");
            new CrateEditor(viewer, crate).open();
        })));
    }

    private void prompt(java.util.function.Consumer<String> onInput) {
        clickSound();
        ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
            onInput.accept(input);
            Services.get().lang().send(viewer, "editor-saved");
            new CrateEditor(viewer, crate).open();
        }, () -> new CrateEditor(viewer, crate).open());
    }

    private void promptNumber(java.util.function.DoubleConsumer onInput) {
        clickSound();
        ChatPrompt.ask(viewer, "editor-prompt-number", input -> {
            try {
                onInput.accept(Double.parseDouble(input.trim()));
                Services.get().lang().send(viewer, "editor-saved");
            } catch (NumberFormatException e) {
                Services.get().lang().send(viewer, "not-a-number",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("input", input));
            }
            new CrateEditor(viewer, crate).open();
        }, () -> new CrateEditor(viewer, crate).open());
    }
}
