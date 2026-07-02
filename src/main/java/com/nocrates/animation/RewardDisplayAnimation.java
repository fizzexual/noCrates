package com.nocrates.animation;

/** Phase 3 of an opening: presents the won reward(s). Call ctx.phaseDone() when finished. */
public interface RewardDisplayAnimation {

    String id();

    void play(OpeningContext ctx);
}
