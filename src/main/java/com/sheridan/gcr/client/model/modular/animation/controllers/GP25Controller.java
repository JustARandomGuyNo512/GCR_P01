package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.GP25Model;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GP25Controller extends AnimationController<GP25Model> {
    @Override
    public void firstPersonSubscriptions(GP25Model model) {
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
    public void initAnimation(GP25Model model) {
        registerAnimations(
                "check", "gcr:ak74m_check_grenade_gp25.g",
                "reload", "gcr:ak74m_reload_grenade_gp25.g"
        );
    }


    @Override
    public void initTrack(GP25Model moduleModel) {
        defineTrack("main").addOnPlayed(instance -> getTrack("check").clear());
        defineTrack("check").addOnApplied((ctx, model) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack("check").clear();
            }
        });
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof GP25Model;
    }

}
