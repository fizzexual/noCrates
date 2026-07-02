package com.nocrates.animation;

import com.nocrates.animation.display.DefaultDisplay;
import com.nocrates.animation.display.FireSpiralDisplay;
import com.nocrates.animation.display.FireworkDisplay;
import com.nocrates.animation.display.HelixDisplay;
import com.nocrates.animation.display.PhysicalItemDisplay;
import com.nocrates.animation.display.SmokeSpiralDisplay;
import com.nocrates.animation.post.BallPost;
import com.nocrates.animation.post.CompactPost;
import com.nocrates.animation.post.FirePost;
import com.nocrates.animation.post.RotatingHeadPost;
import com.nocrates.animation.post.SwirlPost;
import com.nocrates.animation.pre.BlastingPre;
import com.nocrates.animation.pre.CrackPre;
import com.nocrates.animation.pre.DefaultPre;
import com.nocrates.animation.pre.FirePre;
import com.nocrates.animation.pre.KeyOpenerPre;
import com.nocrates.animation.pre.LightningPre;
import com.nocrates.animation.pre.SonicBoomPre;

/** Registers every built-in phase animation and idle shape. */
public final class BuiltinAnimations {

    private BuiltinAnimations() {
    }

    public static void registerAll(AnimationService animations) {
        animations.register(new DefaultPre());
        animations.register(new CrackPre());
        animations.register(new LightningPre());
        animations.register(new KeyOpenerPre());
        animations.register(new FirePre());
        animations.register(new BlastingPre());
        animations.register(new SonicBoomPre());

        animations.register(new BallPost());
        animations.register(new SwirlPost());
        animations.register(new FirePost());
        animations.register(new CompactPost());
        animations.register(new RotatingHeadPost());
        animations.register(new com.nocrates.animation.post.CsgoPost());

        animations.register(new DefaultDisplay());
        animations.register(new com.nocrates.animation.display.BeamDisplay());
        animations.register(new HelixDisplay());
        animations.register(new SmokeSpiralDisplay());
        animations.register(new FireSpiralDisplay());
        animations.register(new PhysicalItemDisplay());
        animations.register(new FireworkDisplay());
        animations.register(new GuiRoulette());

        // the choreographed expansion pack (9 pre + 9 post + 8 display)
        com.nocrates.animation.extra.ExtraPreAnimations.all().forEach(animations::register);
        com.nocrates.animation.extra.ExtraPostAnimations.all().forEach(animations::register);
        com.nocrates.animation.extra.ExtraDisplayAnimations.all().forEach(animations::register);

        ShapeRenderer.registerAll(animations);
    }
}
