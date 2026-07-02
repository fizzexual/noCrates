package com.nocrates.logging;

import com.nocrates.core.MainConfig;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Daily-rotated file log of crate opens and wins, plus Discord webhook fan-out. */
public final class ActionLogger {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Plugin plugin;
    private final MainConfig config;
    private final DiscordWebhook webhook;
    private final File dir;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "noCrates-log");
        t.setDaemon(true);
        return t;
    });

    public ActionLogger(Plugin plugin, MainConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.webhook = new DiscordWebhook(plugin, config);
        this.dir = new File(plugin.getDataFolder(), "logs");
    }

    public void open(String player, String crate) {
        file("OPEN " + player + " opened " + crate);
        if (config.discordLogs("opens")) {
            webhook.send("Crate opened", "**" + player + "** opened **" + crate + "**", 0x7B5CFF);
        }
    }

    public void win(String player, String crate, String reward) {
        file("WIN  " + player + " won " + reward + " from " + crate);
        if (config.discordLogs("wins")) {
            webhook.send("Reward won", "**" + player + "** won **" + reward + "** from **" + crate + "**", 0xFFC145);
        }
    }

    private void file(String line) {
        if (!config.logFile()) return;
        io.submit(() -> {
            try {
                dir.mkdirs();
                File file = new File(dir, LocalDate.now() + ".log");
                String out = "[" + LocalTime.now().format(TIME) + "] " + line + System.lineSeparator();
                Files.writeString(file.toPath(), out, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not write action log: " + e.getMessage());
            }
        });
    }

    public void close() {
        io.shutdown();
        try {
            io.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
