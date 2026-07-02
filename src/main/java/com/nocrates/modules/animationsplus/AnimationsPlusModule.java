package com.nocrates.modules.animationsplus;

import com.nocrates.api.Addon;

/**
 * Animations+ — parity with PhoenixCrates' paid animation add-ons, free: Rainbow,
 * Cyclone Heart, Froggo Boom, Chicken Jockey (pre-open) and Physical CSGO, Orbit
 * Roulette, Astro Burst, Black Hole (post-open). They appear automatically in crate
 * files and the editor's animation pickers.
 */
public final class AnimationsPlusModule extends Addon {

    @Override
    public void onEnable() {
        api().registerPre(new RainbowPre());
        api().registerPre(new CycloneHeartPre());
        api().registerPre(new FroggoBoomPre());
        api().registerPre(new ChickenJockeyPre());
        api().registerPost(new PhysicalCsgoPost());
        api().registerPost(new OrbitRoulettePost());
        api().registerPost(new AstroBurstPost());
        api().registerPost(new BlackHolePost());
        logger().info("Registered 8 extra animations.");
    }
}
