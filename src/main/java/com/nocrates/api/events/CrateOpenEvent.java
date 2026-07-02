package com.nocrates.api.events;

import com.nocrates.crate.Crate;
import com.nocrates.reward.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/** Fired after keys/cost checks pass and the outcome is rolled, before anything is consumed. */
public final class CrateOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final List<Reward> outcome;
    private boolean cancelled;

    public CrateOpenEvent(Player player, Crate crate, List<Reward> outcome) {
        this.player = player;
        this.crate = crate;
        this.outcome = outcome;
    }

    public Player player() {
        return player;
    }

    public Crate crate() {
        return crate;
    }

    /** Mutable: listeners may replace rolled rewards before they are granted. */
    public List<Reward> outcome() {
        return outcome;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
