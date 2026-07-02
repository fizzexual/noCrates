package com.nocrates.migrate;

import com.nocrates.compat.Scheduling;
import com.nocrates.core.Services;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Importer registry + the /crates migrate verb. Addons may register more importers. */
public final class Migrations {

    private static final Map<String, Importer> IMPORTERS = new LinkedHashMap<>();

    private Migrations() {
    }

    public static void register(Importer importer) {
        IMPORTERS.put(importer.id().toLowerCase(Locale.ROOT), importer);
    }

    public static Collection<Importer> all() {
        return IMPORTERS.values();
    }

    public static void registerDefaults() {
        var services = Services.get();
        var pluginsDir = services.plugin().getDataFolder().getParentFile();
        register(new CrazyCratesImporter(pluginsDir, services.crates(), services.keys()));
        register(new ExcellentCratesImporter(pluginsDir, services.crates()));

        com.nocrates.command.CratesCommand.registerExtra("migrate", (sender, args) -> {
            if (!sender.hasPermission("nocrates.admin")) {
                services.lang().send(sender, "no-permission");
                return;
            }
            if (args.length == 0) {
                services.lang().send(sender, "migrate-unknown",
                        Placeholder.unparsed("plugin", ""),
                        Placeholder.unparsed("available", String.join(", ", IMPORTERS.keySet())));
                return;
            }
            run(sender, args[0]);
        });
    }

    public static void run(CommandSender sender, String importerId) {
        var services = Services.get();
        Importer importer = IMPORTERS.get(importerId.toLowerCase(Locale.ROOT));
        if (importer == null) {
            services.lang().send(sender, "migrate-unknown",
                    Placeholder.unparsed("plugin", importerId),
                    Placeholder.unparsed("available", String.join(", ", IMPORTERS.keySet())));
            return;
        }
        if (!importer.detect()) {
            services.lang().send(sender, "migrate-nothing", Placeholder.unparsed("plugin", importer.id()));
            return;
        }
        services.lang().send(sender, "migrate-started", Placeholder.unparsed("plugin", importer.id()));
        // Files are read/written off the main thread, then registries refresh on it.
        Scheduling.async(services.plugin(), () -> {
            ImportReport report = importer.run();
            Scheduling.run(services.plugin(), null, () -> {
                services.placements().rebuild();
                services.lang().send(sender, "migrate-done",
                        Placeholder.unparsed("plugin", importer.id()),
                        Placeholder.unparsed("crates", String.valueOf(report.crates())),
                        Placeholder.unparsed("rewards", String.valueOf(report.rewards())));
                for (String note : report.notes()) {
                    sender.sendMessage(com.nocrates.text.Text.mm("<gray> - " + note));
                }
            });
        });
    }
}
