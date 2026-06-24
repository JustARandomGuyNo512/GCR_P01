package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.TestM203Model;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Controller extends AnimationController<TestM203Model> {
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
                "reload", "gcr:reload_grenade.g"
        );
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
    public void thirdPersonAnimation(TestM203Model model, ModuleRenderContext context) {

    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof TestM203Model;
    }

}
