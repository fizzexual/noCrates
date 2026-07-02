package com.nocrates.animation;

/** Phase 2 of an opening: the burst once the crate opens. Call ctx.phaseDone() when finished. */
public interface PostOpenAnimation {

    String id();

    void play(OpeningContext ctx);
}
