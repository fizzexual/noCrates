package com.nocrates.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** Serialises block locations to/from {@code "world,x,y,z"} strings. */
public final class Locations {

    private Locations() {
    }

    public static String serialize(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + ","
                + location.getBlockY() + "," + location.getBlockZ();
    }

    public static Location parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            String[] parts = value.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }
            return new Location(world,
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim()));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean sameBlock(Location a, Location b) {
        return a != null && b != null
                && a.getWorld() != null && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
