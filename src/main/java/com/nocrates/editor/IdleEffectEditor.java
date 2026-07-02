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

/**
 * Idle-effect builder: existing effects listed (click to remove), plus a wizard that
 * walks shape -> particle -> color/radius and appends the generated spec line.
 */
public final class IdleEffectEditor extends Menu {

    private static final List<String> COMMON_PARTICLES = List.of(
            "DUST", "FLAME", "SOUL_FIRE_FLAME", "END_ROD", "HAPPY_VILLAGER", "ENCHANT",
            "PORTAL", "WITCH", "HEART", "ELECTRIC_SPARK", "SCULK_SOUL", "CHERRY_LEAVES",
            "WAX_ON", "CRIT", "SNOWFLAKE", "GLOW");

    private final Crate crate;
    private String pendingShape;

    public IdleEffectEditor(Player viewer, Crate crate) {
        super(viewer, "<dark_gray>Idle effects <dark_gray>» <white>" + crate.id(), 6);
        this.crate = crate;
    }

    private void save() {
        Services.get().crates().save(crate);
        Services.get().reloads().reloadAll();
    }

    @Override
    protected void draw() {
        var animations = Services.get().animations();
        // current effects
        List<String> effects = crate.animation().idleEffects();
        for (int i = 0; i < effects.size() && i < 9; i++) {
            final String line = effects.get(i);
            set(i, new MenuItem(com.nocrates.item.ItemBuilder.of(Material.PAPER)
                    .name("<yellow>" + line.substring(0, Math.min(40, line.length())))
                    .lore(List.of("<red>Click to remove")).build(), e -> {
                clickSound();
                List<String> updated = new ArrayList<>(crate.animation().idleEffects());
                updated.remove(line);
                crate.animation().idleEffects(updated);
                save();
                refresh();
            }));
        }

        if (pendingShape == null) {
            // step 1: pick a shape
            int slot = 18;
            for (String shape : animations.shapeIds()) {
                if (slot >= 45) break;
                set(slot++, new MenuItem(com.nocrates.item.ItemBuilder.of(Material.FIREWORK_STAR)
                        .name("<yellow>" + shape).lore(List.of("<gray>Step 1/2: pick the shape")).build(), e -> {
                    clickSound();
                    pendingShape = shape;
                    refresh();
                }));
            }
        } else {
            // step 2: pick a particle
            int slot = 18;
            for (String particle : COMMON_PARTICLES) {
                if (slot >= 45) break;
                final String chosen = particle;
                set(slot++, new MenuItem(com.nocrates.item.ItemBuilder.of(Material.BLAZE_POWDER)
                        .name("<yellow>" + particle)
                        .lore(List.of("<gray>Step 2/2: pick the particle",
                                "<gray>shape: <white>" + pendingShape)).build(), e -> {
                    clickSound();
                    if (chosen.equals("DUST")) {
                        ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
                            String hex = input.trim().matches("#[0-9a-fA-F]{6}") ? input.trim() : "#7b5cff";
                            add(pendingShape + ";{DUST;" + hex + ";0;0.2;0;1.2;0;2}");
                            new IdleEffectEditor(viewer, crate).open();
                        }, () -> new IdleEffectEditor(viewer, crate).open());
                    } else {
                        add(pendingShape + ";{" + chosen + ";;0;0.2;0;1.2;0.01;1}");
                        refresh();
                    }
                    pendingShape = null;
                }));
            }
            set(49, new MenuItem(EditorIcons.button(Material.BARRIER, "Cancel shape choice"), e -> {
                clickSound();
                pendingShape = null;
                refresh();
            }));
        }
        set(45, new MenuItem(EditorIcons.back(), e -> new CrateEditor(viewer, crate).open()));
    }

    private void add(String spec) {
        List<String> updated = new ArrayList<>(crate.animation().idleEffects());
        updated.add(spec);
        crate.animation().idleEffects(updated);
        save();
        Services.get().lang().send(viewer, "editor-saved");
        preview(spec);
    }

    /** Renders the freshly added effect at the player for ~3 seconds. */
    private void preview(String specLine) {
        com.nocrates.animation.EffectSpec spec;
        try {
            spec = com.nocrates.animation.EffectSpec.parse(specLine);
        } catch (IllegalArgumentException e) {
            return;
        }
        var services = Services.get();
        var shape = services.animations().shape(spec.shape());
        if (shape == null) return;
        var anchor = viewer.getLocation().clone().add(0, 1.1, 0);
        final int[] tick = {0};
        final com.nocrates.compat.Scheduling.Cancellable[] handle = new com.nocrates.compat.Scheduling.Cancellable[1];
        handle[0] = com.nocrates.compat.Scheduling.timer(services.plugin(), anchor, 2, 2, () -> {
            tick[0] += 2;
            com.nocrates.animation.ParticleBrush.render(spec, shape, anchor, tick[0]);
            if (tick[0] >= 60 && handle[0] != null) handle[0].cancel();
        });
    }
}
