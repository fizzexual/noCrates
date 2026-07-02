package com.nocrates.open;

import com.nocrates.core.Services;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuConfig;
import com.nocrates.menu.MenuItem;
import com.nocrates.reward.Reward;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * Post-animation claim/reroll choice. Closing the menu (or going offline) claims the
 * current reward — a reroll can never be used to dodge a grant.
 */
public final class RerollMenu extends Menu {

    private final OpenSession session;
    private final MenuConfig.Spec spec;

    public RerollMenu(OpenSession session) {
        super(session.player(), Services.get().menus().get("reroll").title()
                .replace("<crate>", session.crate().displayName()), Services.get().menus().get("reroll").rows());
        this.session = session;
        this.spec = Services.get().menus().get("reroll");
    }

    @Override
    protected void draw() {
        Reward current = session.outcome().get(0);
        MenuConfig.Icon rewardIcon = spec.icon("reward");
        if (rewardIcon != null) {
            for (int slot : rewardIcon.slots()) {
                set(slot, new MenuItem(current.displayItem().build()));
            }
        }
        MenuConfig.Icon accept = spec.icon("accept");
        if (accept != null) {
            for (int slot : accept.slots()) {
                set(slot, new MenuItem(accept.item().build(), e -> {
                    clickSound();
                    viewer.closeInventory(); // onClose grants
                }));
            }
        }
        MenuConfig.Icon reroll = spec.icon("reroll");
        if (reroll != null) {
            var services = Services.get();
            int available = services.rerolls().available(viewer, session.crate(), session.temporaryRerolls());
            var item = reroll.item().copy();
            item.lore(item.lore().stream()
                    .map(l -> l.replace("<rerolls>", String.valueOf(available))).toList());
            for (int slot : reroll.slots()) {
                set(slot, new MenuItem(item.build(), e -> {
                    clickSound();
                    doReroll();
                }));
            }
        }
    }

    private void doReroll() {
        var services = Services.get();
        int left = services.rerolls().consume(viewer, session.crate(), session.temporaryRerolls());
        if (left < 0) {
            services.lang().send(viewer, "reroll-none");
            return;
        }
        session.temporaryRerolls(left);
        Reward previous = session.outcome().get(0);
        Reward replacement = services.openService().rollReplacement(viewer, session.crate(), previous);
        if (replacement == null) replacement = previous;
        var data = services.players().of(viewer);
        boolean alternative = !services.openService().isAllowed(viewer, data, session.crate(), replacement);
        session.replaceOutcome(0, replacement, alternative);
        services.lang().send(viewer, "reroll-used",
                Placeholder.parsed("reward", replacement.displayName()));
        refresh();
    }

    @Override
    protected void onClose() {
        session.grantAll();
    }
}
