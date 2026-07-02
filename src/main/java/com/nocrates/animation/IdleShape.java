package com.nocrates.animation;

import org.bukkit.util.Vector;

import java.util.List;

/** A parametric particle shape for idle effects; returns offsets from the crate anchor. */
public interface IdleShape {

    String id();

    /** Points to render this tick (animation time = tick, radius from the effect spec). */
    List<Vector> points(double radius, int tick);
}
