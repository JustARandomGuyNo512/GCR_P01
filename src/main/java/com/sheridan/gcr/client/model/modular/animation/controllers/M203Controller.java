package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.M203Model;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Controller extends AnimationController<M203Model> {
    @Override
    public void firstPersonSubscriptions(M203Model model) {
        super.firstPersonSubscriptions(model);
        subscribe(EventType.CHECK_SUB_WEAPON, 0, (ctx) -> {
            if (isTrackClear("main")) {
                getTrack("check").play(anim("check"));
            }
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 0, (ctx) -> {
            getTrack("main").play(anim("reload").coverStateExclude("shell"));
        });

        subscribe(EventType.SHOOT, 0, (ctx) -> getTrack("check").clear());
    }

    @Override
    public void initAnimation(M203Model model) {
        registerAnimations(
                "check", "gcr:check_grenade_m203.g",
                "reload", "gcr:reload_grenade_m203.g",
                "prepare", "gcr:m203_prepare"
        );
    }


    @Override
    public void initTrack(M203Model moduleModel) {
        defineTrack("main").addOnPlayed(instance -> getTrack("check").clear());
        defineTrack("check").addOnApplied((ctx, model) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack("check").clear();
            }
        });
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof M203Model;
    }

}
