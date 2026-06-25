package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationInstance;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.TestM203Model;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Controller extends AnimationController<TestM203Model> {
    private AnimationDef prepare;
    @Override
    public void firstPersonSubscriptions(TestM203Model model) {
        subscribe(EventType.CHECK_SUB_WEAPON, 0, (ctx) -> {
            if (isTrackClear("main")) {
                getTrack("check").play(anim("check"));
            }
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 0, (ctx) -> {
            getTrack("main").play(anim("reload"));
        });
    }

    @Override
    public void initAnimation(TestM203Model model) {
        registerAnimations(
                "check", "gcr:check_grenade.g",
                "reload", "gcr:reload_grenade.g",
                "prepare", "gcr:m203_prepare"
        );
        prepare = anim("prepare").animation;
    }

    @Override
    public void customFirstPersonAnimation(TestM203Model model, FirstPersonRenderContext context) {

    }

    @Override
    public void initTrack(TestM203Model moduleModel) {
        defineTrack("main").addOnPlayed(instance -> getTrack("check").clear());
        defineTrack("check").addOnApplied((ctx, model) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack("check").clear();
            }
        });
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof TestM203Model;
    }

}
