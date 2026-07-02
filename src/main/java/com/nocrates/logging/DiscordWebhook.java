package com.nocrates.logging;

import com.nocrates.core.MainConfig;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/** Fire-and-forget Discord webhook embeds with basic 429 backoff. */
public final class DiscordWebhook {

    private final Plugin plugin;
    private final MainConfig config;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private volatile long mutedUntil;

    public DiscordWebhook(Plugin plugin, MainConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void send(String title, String description, int color) {
        if (!config.discordEnabled()) return;
        String url = config.discordWebhook();
        if (url == null || url.isEmpty() || System.currentTimeMillis() < mutedUntil) return;
        String json = "{\"username\":" + quote(config.discordUsername())
                + ",\"embeds\":[{\"title\":" + quote(title)
                + ",\"description\":" + quote(description)
                + ",\"color\":" + color
                + ",\"timestamp\":" + quote(Instant.now().toString()) + "}]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((res, err) -> {
            if (err != null) {
                plugin.getLogger().warning("Discord webhook failed: " + err.getMessage());
                mutedUntil = System.currentTimeMillis() + 60_000;
            } else if (res.statusCode() == 429) {
                mutedUntil = System.currentTimeMillis() + 30_000;
            }
        });
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
