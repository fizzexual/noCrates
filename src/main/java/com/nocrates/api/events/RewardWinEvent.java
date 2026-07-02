package com.nocrates.api.events;

import com.nocrates.crate.Crate;
import com.nocrates.reward.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired once per reward as it is granted (after items/commands executed). */
public final class RewardWinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final Reward reward;
    private final boolean alternative;

    public RewardWinEvent(Player player, Crate crate, Reward reward, boolean alternative) {
        this.player = player;
        this.crate = crate;
        this.reward = reward;
        this.alternative = alternative;
    }

    public Player player() {
        return player;
    }

    public Crate crate() {
        return crate;
    }

    public Reward reward() {
        return reward;
    }

    /** True when the alternative reward was granted instead of the reward itself. */
    public boolean alternative() {
        return alternative;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
