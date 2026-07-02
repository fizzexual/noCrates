package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

// (live previews delegate to AnimationDemo)

/**
 * Per-phase animation selection. Lists every registered animation — including ones
 * added by modules and external addons — for pre-open, post-open and reward-display.
 */
public final class AnimationPicker extends Menu {

    private enum PhaseTab {
        PRE, POST, DISPLAY
    }

    private final Crate crate;
    private PhaseTab tab = PhaseTab.PRE;

    public AnimationPicker(Player viewer, Crate crate) {
        super(viewer, "<dark_gray>Animations <dark_gray>» <white>" + crate.id(), 6);
        this.crate = crate;
    }

    private void save() {
        Services.get().crates().save(crate);
    }

    @Override
    protected void draw() {
        var animations = Services.get().animations();
        set(0, tabIcon(PhaseTab.PRE, "Pre-open", crate.animation().preOpen()));
        set(1, tabIcon(PhaseTab.POST, "Post-open", crate.animation().postOpen()));
        set(2, tabIcon(PhaseTab.DISPLAY, "Reward display", crate.animation().rewardDisplay()));

        set(7, new MenuItem(EditorIcons.button(Material.CLOCK, "Phase timings",
                "pre " + crate.animation().preDelayTicks() + "t / post "
                        + crate.animation().postDelayTicks() + "t / display "
                        + crate.animation().displayDurationTicks() + "t",
                "Click and type: <pre> <post> <display>", "in ticks (20 = 1 second)."), e -> {
            clickSound();
            ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
                String[] parts = input.trim().split("\\s+");
                try {
                    if (parts.length >= 1) crate.animation().preDelayTicks(Integer.parseInt(parts[0]));
                    if (parts.length >= 2) crate.animation().postDelayTicks(Integer.parseInt(parts[1]));
                    if (parts.length >= 3) crate.animation().displayDurationTicks(Integer.parseInt(parts[2]));
                    save();
                    Services.get().lang().send(viewer, "editor-saved");
                } catch (NumberFormatException ex) {
                    Services.get().lang().send(viewer, "editor-invalid");
                }
                new AnimationPicker(viewer, crate).open();
            }, () -> new AnimationPicker(viewer, crate).open());
        }));

        List<String> ids = new ArrayList<>(switch (tab) {
            case PRE -> animations.preIds();
            case POST -> animations.postIds();
            case DISPLAY -> animations.displayIds();
        });
        String current = switch (tab) {
            case PRE -> crate.animation().preOpen();
            case POST -> crate.animation().postOpen();
            case DISPLAY -> crate.animation().rewardDisplay();
        };
        for (int i = 0; i < ids.size() && i < 36; i++) {
            String id = ids.get(i);
            boolean selected = id.equalsIgnoreCase(current);
            boolean demoable = AnimationDemo.canDemo(id);
            List<String> lore = new ArrayList<>(List.of(selected ? "<green>Selected" : "<gray>Click to select"));
            if (demoable) lore.add("<light_purple>Right-click for a live preview");
            var icon = com.nocrates.item.ItemBuilder
                    .of(selected ? Material.GLOWSTONE_DUST : Material.GUNPOWDER)
                    .name((selected ? "<green>" : "<yellow>") + id)
                    .lore(lore)
                    .glow(selected)
                    .build();
            set(9 + i, new MenuItem(icon, e -> {
                clickSound();
                if (e.isRightClick()) {
                    if (!demoable) return;
                    viewer.closeInventory();
                    AnimationDemo.play(viewer, crate, switch (tab) {
                        case PRE -> com.nocrates.animation.OpeningContext.Phase.PRE;
                        case POST -> com.nocrates.animation.OpeningContext.Phase.POST;
                        case DISPLAY -> com.nocrates.animation.OpeningContext.Phase.DISPLAY;
                    }, id);
                    return;
                }
                switch (tab) {
                    case PRE -> crate.animation().preOpen(id);
                    case POST -> crate.animation().postOpen(id);
                    case DISPLAY -> crate.animation().rewardDisplay(id);
                }
                save();
                refresh();
            }));
        }
        set(45, new MenuItem(EditorIcons.back(), e -> new CrateEditor(viewer, crate).open()));
    }

    private MenuItem tabIcon(PhaseTab which, String label, String current) {
        boolean active = tab == which;
        var icon = com.nocrates.item.ItemBuilder
                .of(active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name((active ? "<green>" : "<gray>") + label)
                .lore(List.of("<gray>Current: <white>" + current))
                .build();
        return new MenuItem(icon, e -> {
            clickSound();
            tab = which;
            refresh();
        });
    }
}
