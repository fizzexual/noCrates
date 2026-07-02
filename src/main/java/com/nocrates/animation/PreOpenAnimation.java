package com.nocrates.animation;

/** Phase 1 of an opening: plays at the crate before the reveal. Call ctx.phaseDone() when finished. */
public interface PreOpenAnimation {

    String id();

    void play(OpeningContext ctx);
}
