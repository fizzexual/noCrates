package com.nocrates.hook;

import org.bukkit.entity.Player;

/** noCrates' own permission facade backed by Vault when present. */
public interface Permissions {

    boolean available();

    void add(Player player, String node);
}
