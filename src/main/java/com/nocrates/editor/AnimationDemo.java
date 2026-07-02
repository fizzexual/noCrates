package com.nocrates.editor;

import com.nocrates.animation.OpeningContext;
import com.nocrates.core.Services;
import com.nocrates.crate.Crate;
import com.nocrates.item.ItemSpec;
import com.nocrates.reward.Reward;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Live animation previews for the editor: plays one phase animation anchored at the
 * editing player with a throwaway outcome. Nothing is granted — the demo context's
 * completion is a no-op, and cleanups still run when the animation finishes.
 */
final class AnimationDemo {

    private AnimationDemo() {
    }

    /** Phase animations that must not be demoed (they have real side effects). */
    private static final List<String> UNSAFE = List.of("CHEST_HUNT");

    static boolean canDemo(String id) {
        return !UNSAFE.contains(id.toUpperCase(Locale.ROOT));
    }

    static void play(Player player, Crate crate, OpeningContext.Phase phase, String id) {
        var services = Services.get();
        var animations = services.animations();
        List<Reward> outcome = new ArrayList<>();
        if (!crate.rewardList().isEmpty()) {
            outcome.add(crate.rewardList().get(0));
        } else {
            Reward demo = new Reward("demo");
            demo.displayItem(new ItemSpec("DIAMOND").name("<aqua>Demo Reward"));
            outcome.add(demo);
        }
        OpeningContext ctx = new OpeningContext(services.plugin(), player, crate, null, outcome, () -> {
        });
        String key = id.toUpperCase(Locale.ROOT);
        switch (phase) {
            case PRE -> animations.preById(key).ifPresent(a -> a.play(ctx));
            case POST -> animations.postById(key).ifPresent(a -> a.play(ctx));
            case DISPLAY -> animations.displayById(key).ifPresent(a -> a.play(ctx));
            default -> {
            }
        }
    }
}
