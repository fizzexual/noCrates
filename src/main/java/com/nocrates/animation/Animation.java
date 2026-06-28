package com.nocrates.animation;

/**
 * A crate opening style. The outcome is rolled <em>before</em> the animation
 * runs; the animation only visualises {@link CrateSession#outcome()} and must
 * call {@link CrateSession#finish()} exactly once when done (the controller
 * then grants the rewards). This keeps visuals and reward-granting separate.
 */
public interface Animation {

    String id();

    void play(CrateSession session);
}
