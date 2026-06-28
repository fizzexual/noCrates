package com.nocrates.animation;

import com.nocrates.NoCrates;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps animation ids to implementations. Unknown or not-yet-implemented ids
 * fall back to {@code reveal} so a crate is always openable.
 */
public final class AnimationRegistry {

    private final Map<String, Animation> byId = new HashMap<>();
    private final Animation fallback;

    public AnimationRegistry(NoCrates plugin) {
        register(new InstantAnimation());
        register(new RevealAnimation(plugin));
        register(new CsgoAnimation(plugin));
        this.fallback = byId.get("reveal");
    }

    public void register(Animation animation) {
        byId.put(animation.id().toLowerCase(Locale.ROOT), animation);
    }

    public Animation get(String id) {
        if (id == null) {
            return fallback;
        }
        return byId.getOrDefault(id.toLowerCase(Locale.ROOT), fallback);
    }

    public boolean has(String id) {
        return id != null && byId.containsKey(id.toLowerCase(Locale.ROOT));
    }
}
