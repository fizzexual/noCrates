package com.nocrates.animation;

/** No animation — grants immediately. */
public final class InstantAnimation implements Animation {

    @Override
    public String id() {
        return "instant";
    }

    @Override
    public void play(CrateSession session) {
        session.finish();
    }
}
