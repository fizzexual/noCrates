package com.nocrates.crate;

import java.util.List;

/**
 * Optional physical placement of a crate: world block locations, hologram lines
 * shown above them, and an ambient particle. Locations are stored as
 * {@code "world,x,y,z"} strings.
 */
public final class CrateBlock {

    private final boolean enabled;
    private final List<String> locations;
    private final List<String> hologram;
    private final String particle;

    public CrateBlock(boolean enabled, List<String> locations, List<String> hologram, String particle) {
        this.enabled = enabled;
        this.locations = locations == null ? List.of() : List.copyOf(locations);
        this.hologram = hologram == null ? List.of() : List.copyOf(hologram);
        this.particle = particle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> locations() {
        return locations;
    }

    public List<String> hologram() {
        return hologram;
    }

    public String particle() {
        return particle;
    }
}
