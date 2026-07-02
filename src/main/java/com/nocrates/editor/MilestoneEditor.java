package com.nocrates.editor;

import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.menu.ChatPrompt;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.GuaranteedWin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Guaranteed-win (milestones) editor: mode, enable, add/remove milestones. */
public final class MilestoneEditor extends Menu {

    private final Crate crate;

    public MilestoneEditor(Player viewer, Crate crate) {
        super(viewer, "<dark_gray>Guaranteed win <dark_gray>» <white>" + crate.id(), 6);
        this.crate = crate;
    }

    private void save() {
        Services.get().crates().save(crate);
    }

    @Override
    protected void draw() {
        set(0, new MenuItem(EditorIcons.toggle("Guaranteed win", crate.guaranteedEnabled()), e -> {
            clickSound();
            crate.guaranteedEnabled(!crate.guaranteedEnabled());
            save();
            refresh();
        }));
        set(1, new MenuItem(EditorIcons.button(Material.REPEATER,
                "Mode: " + crate.guaranteedMode().name(),
                "REPETITIVE fires every N openings.",
                "SEQUENTIAL walks the list once."), e -> {
            clickSound();
            crate.guaranteedMode(crate.guaranteedMode() == GuaranteedWin.Mode.REPETITIVE
                    ? GuaranteedWin.Mode.SEQUENTIAL : GuaranteedWin.Mode.REPETITIVE);
            save();
            refresh();
        }));
        set(8, new MenuItem(EditorIcons.button(Material.NETHER_STAR, "Add milestone",
                "Type: <openings> <rewardId> [chance]",
                "e.g. \"25 vip_rank\" or \"0 bonus 10\""), e -> {
            clickSound();
            ChatPrompt.ask(viewer, "editor-prompt-text", input -> {
                String[] parts = input.trim().split("\\s+");
                try {
                    int openings = Integer.parseInt(parts[0]);
                    String rewardId = parts.length > 1 ? parts[1] : "";
                    double chance = parts.length > 2 ? Double.parseDouble(parts[2]) : 0;
                    if (crate.reward(rewardId) == null) {
                        Services.get().lang().send(viewer, "reward-not-found",
                                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("reward", rewardId));
                    } else {
                        List<GuaranteedWin.Milestone> updated = new ArrayList<>(crate.milestones());
                        updated.add(new GuaranteedWin.Milestone(openings, rewardId, chance));
                        crate.milestones(updated);
                        save();
                        Services.get().lang().send(viewer, "editor-saved");
                    }
                } catch (Exception ex) {
                    Services.get().lang().send(viewer, "editor-invalid");
                }
                new MilestoneEditor(viewer, crate).open();
            }, () -> new MilestoneEditor(viewer, crate).open());
        }));

        List<GuaranteedWin.Milestone> milestones = crate.milestones();
        for (int i = 0; i < milestones.size() && i < 36; i++) {
            GuaranteedWin.Milestone milestone = milestones.get(i);
            // color hints: openings-only (red), chance-only (green), or both (yellow)
            Material material = milestone.openings() > 0 && milestone.chance() > 0 ? Material.YELLOW_WOOL
                    : milestone.openings() > 0 ? Material.RED_WOOL : Material.LIME_WOOL;
            final int index = i;
            set(9 + i, new MenuItem(com.nocrates.item.ItemBuilder.of(material)
                    .name("<yellow>#" + (i + 1) + " <white>" + milestone.rewardId())
                    .lore(List.of(
                            "<gray>openings: <white>" + milestone.openings(),
                            "<gray>chance: <white>" + (milestone.chance() > 0 ? milestone.chance() + "%" : "always"),
                            "<red>Click to remove"))
                    .build(), e -> {
                clickSound();
                List<GuaranteedWin.Milestone> updated = new ArrayList<>(crate.milestones());
                updated.remove(index);
                crate.milestones(updated);
                save();
                refresh();
            }));
        }
        set(45, new MenuItem(EditorIcons.back(), e -> new CrateEditor(viewer, crate).open()));
    }
}
