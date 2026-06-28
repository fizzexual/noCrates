package com.nocrates.crate;

import com.nocrates.key.KeyType;
import com.nocrates.reward.Pity;
import com.nocrates.reward.Reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An immutable crate definition. Build via {@link #builder(String)}; the editor
 * and serializer reconstruct crates through the same builder so there is one
 * construction path.
 */
public final class Crate {

    private final String name;
    private final String displayName;
    private final String animation;
    private final CrateKeySpec key;
    private final CrateBlock block;
    private final boolean previewEnabled;
    private final String previewTitle;
    private final Pity pity;
    private final boolean broadcast;
    private final int cooldownSeconds;
    private final String openSound;
    private final List<Reward> rewards;

    private Crate(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.animation = builder.animation;
        this.key = builder.key;
        this.block = builder.block;
        this.previewEnabled = builder.previewEnabled;
        this.previewTitle = builder.previewTitle;
        this.pity = builder.pity;
        this.broadcast = builder.broadcast;
        this.cooldownSeconds = builder.cooldownSeconds;
        this.openSound = builder.openSound;
        this.rewards = List.copyOf(builder.rewards);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }

    public String animation() {
        return animation;
    }

    public CrateKeySpec key() {
        return key;
    }

    /** Physical placement, or {@code null} for a virtual-only crate. */
    public CrateBlock block() {
        return block;
    }

    public boolean previewEnabled() {
        return previewEnabled;
    }

    public String previewTitle() {
        return previewTitle;
    }

    public Pity pity() {
        return pity;
    }

    public boolean broadcast() {
        return broadcast;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    /** Per-crate open sound spec, or {@code null} to use the global default. */
    public String openSound() {
        return openSound;
    }

    public List<Reward> rewards() {
        return rewards;
    }

    public static final class Builder {

        private final String name;
        private String displayName;
        private String animation = "reveal";
        private CrateKeySpec key;
        private CrateBlock block;
        private boolean previewEnabled = true;
        private String previewTitle;
        private Pity pity = Pity.disabled();
        private boolean broadcast;
        private int cooldownSeconds;
        private String openSound;
        private final List<Reward> rewards = new ArrayList<>();

        Builder(String name) {
            this.name = name.toLowerCase(Locale.ROOT);
            this.displayName = name;
            this.previewTitle = name + " — Rewards";
            this.key = new CrateKeySpec(KeyType.VIRTUAL, this.name, null);
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder animation(String animation) {
            this.animation = animation;
            return this;
        }

        public Builder key(CrateKeySpec key) {
            this.key = key;
            return this;
        }

        public Builder block(CrateBlock block) {
            this.block = block;
            return this;
        }

        public Builder preview(boolean enabled, String title) {
            this.previewEnabled = enabled;
            this.previewTitle = title;
            return this;
        }

        public Builder pity(Pity pity) {
            this.pity = pity;
            return this;
        }

        public Builder broadcast(boolean broadcast) {
            this.broadcast = broadcast;
            return this;
        }

        public Builder cooldownSeconds(int cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
            return this;
        }

        public Builder openSound(String openSound) {
            this.openSound = openSound;
            return this;
        }

        public Builder addReward(Reward reward) {
            this.rewards.add(reward);
            return this;
        }

        public Builder rewards(List<Reward> rewards) {
            this.rewards.clear();
            this.rewards.addAll(rewards);
            return this;
        }

        public Crate build() {
            return new Crate(this);
        }
    }
}
