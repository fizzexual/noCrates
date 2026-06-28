package com.nocrates.reward;

import org.bukkit.entity.Player;

/** A single executable effect granted to a player when a reward is won. */
@FunctionalInterface
public interface RewardAction {

    void execute(Player player);
}
