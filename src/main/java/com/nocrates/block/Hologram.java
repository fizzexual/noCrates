package com.nocrates.block;

import com.nocrates.util.Items;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * A floating multi-line label built from native {@link TextDisplay} entities
 * (stable since 1.19.4), so no hologram plugin is required. Entities are
 * non-persistent and managed by {@link CrateBlockManager}.
 */
public final class Hologram {

    private static final double LINE_SPACING = 0.28;

    private final List<TextDisplay> lines = new ArrayList<>();

    public void spawn(Location base, List<String> textLines) {
        remove();
        if (base.getWorld() == null || textLines.isEmpty()) {
            return;
        }
        double startY = base.getY() + 1.5 + LINE_SPACING * (textLines.size() - 1);
        for (int i = 0; i < textLines.size(); i++) {
            String text = textLines.get(i);
            Location location = new Location(base.getWorld(),
                    base.getBlockX() + 0.5, startY - i * LINE_SPACING, base.getBlockZ() + 0.5);
            TextDisplay display = base.getWorld().spawn(location, TextDisplay.class, entity -> {
                entity.text(Items.mini(text));
                entity.setBillboard(Display.Billboard.CENTER);
                try {
                    entity.setShadowed(false);
                    entity.setDefaultBackground(false);
                    entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    entity.setPersistent(false);
                } catch (Throwable ignored) {
                    // display-tuning API varies by version; the text still shows
                }
            });
            lines.add(display);
        }
    }

    public void remove() {
        for (TextDisplay display : lines) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        lines.clear();
    }
}
