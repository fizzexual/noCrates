package com.nocrates.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/** Fired when a player's virtual key balance changes through the key service. */
public final class KeyChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final String keyId;
    private final int delta;

    public KeyChangeEvent(UUID player, String keyId, int delta) {
        this.player = player;
        this.keyId = keyId;
        this.delta = delta;
    }

    public UUID player() {
        return player;
    }

    public String keyId() {
        return keyId;
    }

    public int delta() {
        return delta;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
