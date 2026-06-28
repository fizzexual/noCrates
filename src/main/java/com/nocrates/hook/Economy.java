package com.nocrates.hook;

import org.bukkit.entity.Player;

/** noCrates' own economy facade so reward actions never reference Vault types directly. */
public interface Economy {

    boolean available();

    void deposit(Player player, double amount);
}
