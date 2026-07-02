package com.nocrates.modules.massopen;

import com.nocrates.api.events.CrateOpenEvent;
import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemBuilder;
import com.nocrates.key.KeyLink;
import com.nocrates.menu.Menu;
import com.nocrates.menu.MenuItem;
import com.nocrates.open.OpenService;
import com.nocrates.reward.Reward;
import com.nocrates.reward.RewardGrant;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tier picker + batch opening with an aggregated summary. */
public final class MassOpenMenu extends Menu {

    private final Crate crate;
    private final MassOpenModule module;

    public MassOpenMenu(Player viewer, Crate crate, MassOpenModule module) {
        super(viewer, Services.get().menus().get("massopen").title().replace("<crate>", crate.displayName()),
              Services.get().menus().get("massopen").rows());
        this.crate = crate;
        this.module = module;
    }

    @Override
    protected void draw() {
        var spec = Services.get().menus().get("massopen");
        List<Integer> slots = spec.contentSlots().isEmpty()
                ? List.of(10, 11, 12, 13, 14, 15, 16) : spec.contentSlots();
        int i = 0;
        for (int tier : module.tiers()) {
            if (i >= slots.size()) break;
            if (!hasTierPermission(String.valueOf(tier))) continue;
            set(slots.get(i++), new MenuItem(tierIcon("x" + tier, tier), e -> {
                clickSound();
                viewer.closeInventory();
                massOpen(tier);
            }));
        }
        if (module.allowAll() && i < slots.size() && hasTierPermission("all")) {
            set(slots.get(i), new MenuItem(tierIcon("ALL", maxAffordable()), e -> {
                clickSound();
                viewer.closeInventory();
                massOpen(maxAffordable());
            }));
        }
        var close = spec.icon("close");
        if (close != null) {
            for (int slot : close.slots()) {
                set(slot, new MenuItem(close.item().build(), e -> viewer.closeInventory()));
            }
        }
    }

    private boolean hasTierPermission(String tier) {
        return viewer.hasPermission("nocrates.massopen." + tier)
                || viewer.hasPermission("nocrates.massopen.*")
                || viewer.hasPermission("nocrates.admin");
    }

    private org.bukkit.inventory.ItemStack tierIcon(String label, int amount) {
        return ItemBuilder.of(Material.TRIPWIRE_HOOK)
                .name("<yellow><bold>" + label + "</bold>")
                .lore(List.of("<gray>Opens <yellow>" + Math.max(0, amount) + "</yellow> key(s) at once"))
                .amount(Math.max(1, Math.min(64, amount)))
                .glow(true)
                .build();
    }

    /** Highest opening count the player's keys can cover (for the ALL tier). */
    private int maxAffordable() {
        var services = Services.get();
        int best = 0;
        for (int n = 1; n <= module.allCap(); n++) {
            if (!services.keyService().has(viewer, scaled(n))) break;
            best = n;
        }
        return best;
    }

    private List<KeyLink> scaled(int times) {
        List<KeyLink> scaled = new ArrayList<>();
        for (KeyLink link : crate.keys()) {
            scaled.add(new KeyLink(link.keyId(), link.amount() * times, link.priority()));
        }
        return scaled;
    }

    private void massOpen(int times) {
        var services = Services.get();
        // same gates as a normal open — mass-open must not be a side door
        if (!crate.enabled()) {
            services.lang().send(viewer, "open-crate-disabled");
            return;
        }
        if (crate.permissionRequired() && !viewer.hasPermission(crate.permission())) {
            services.lang().send(viewer, "open-no-crate-permission");
            return;
        }
        var data = services.players().of(viewer);
        long now = java.time.Instant.now().getEpochSecond();
        if (data.cooldownUntil(crate.id()) > now) {
            services.lang().send(viewer, "open-cooldown", Placeholder.unparsed("time",
                    com.nocrates.text.Times.format(services.lang(), data.cooldownUntil(crate.id()) - now)));
            return;
        }
        if (times <= 0 || !services.keyService().has(viewer, scaled(times))) {
            services.lang().send(viewer, "massopen-no-keys",
                    Placeholder.unparsed("amount", String.valueOf(Math.max(1, times))));
            return;
        }
        // money up front (refunded for opens that don't happen), keys PER OPEN so a
        // mid-batch stop can never eat keys for openings that never occurred
        double costPerOpen = crate.open().cost();
        boolean charging = costPerOpen > 0 && com.nocrates.hook.VaultHook.ready();
        if (charging && !com.nocrates.hook.VaultHook.withdraw(viewer, costPerOpen * times)) {
            services.lang().send(viewer, "open-not-enough-money",
                    Placeholder.unparsed("cost", com.nocrates.hook.VaultHook.format(costPerOpen * times)));
            return;
        }
        OpenService open = services.openService();
        Map<String, Integer> counts = new LinkedHashMap<>();
        int successful = 0;
        for (int i = 0; i < times; i++) {
            OpenService.Rolled rolled = open.rollOutcome(viewer, crate, crate.maxWinRewards());
            if (rolled == null) break;
            CrateOpenEvent event = new CrateOpenEvent(viewer, crate, rolled.rewards());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            open.recomputeAlternatives(viewer, crate, rolled);
            if (!services.keyService().consume(viewer, scaled(1))) break;
            successful++;
            data.incrOpens(crate.id());
            for (int r = 0; r < rolled.rewards().size(); r++) {
                Reward reward = rolled.rewards().get(r);
                boolean alternative = r < rolled.alternative().length && rolled.alternative()[r];
                if (!alternative) services.winLimits().record(data, crate, reward);
                RewardGrant.grant(viewer, crate, reward, alternative, true);
                counts.merge(reward.displayName(), 1, Integer::sum);
            }
        }
        if (charging) {
            if (successful < times) {
                com.nocrates.hook.VaultHook.deposit(viewer, costPerOpen * (times - successful));
            }
            if (successful > 0) {
                services.lang().send(viewer, "open-cost-charged", Placeholder.unparsed("cost",
                        com.nocrates.hook.VaultHook.format(costPerOpen * successful)));
            }
        }
        if (successful > 0 && crate.open().cooldownSeconds() > 0) {
            data.setCooldown(crate.id(), now + crate.open().cooldownSeconds());
        }
        services.actionLogger().open(viewer.getName(), crate.id() + " x" + successful);
        services.lang().send(viewer, "massopen-summary-header",
                Placeholder.unparsed("amount", String.valueOf(successful)),
                Placeholder.parsed("crate", crate.displayName()));
        for (Map.Entry<String, Integer> line : counts.entrySet()) {
            services.lang().sendRaw(viewer, "massopen-summary-line",
                    Placeholder.parsed("reward", line.getKey()),
                    Placeholder.unparsed("amount", String.valueOf(line.getValue())));
        }
    }
}
