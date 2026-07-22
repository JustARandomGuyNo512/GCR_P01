package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.animation.SingleAnimationSequence;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.AKModel;
import com.sheridan.gcr.client.model.modular.modules.ARMainModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.AKView;
import com.sheridan.gcr.modularSys.modules.views.ARView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AKController extends AnimationController<AKModel> {
    private SingleAnimationSequence shoot;
    private SingleAnimationSequence shootLast;
    private SingleAnimationSequence shootStuck;
    private SingleAnimationSequence shootLastStuck;
    private AnimationDef thirdPersonShoot;
    @Override
    public void firstPersonSubscriptions(AKModel model) {
        super.firstPersonSubscriptions(model);
        AKView view = model.getView();

        shoot = new SingleAnimationSequence(anim("shoot").coverState());
        shootLast = new SingleAnimationSequence(anim("shoot_last").coverState());
        shootStuck = new SingleAnimationSequence(anim("shoot_stuck").coverState());
        shootLastStuck = new SingleAnimationSequence(anim("shoot_last_stuck").coverState());

        thirdPersonShoot = anim("shoot").animation;

        subscribe(EventType.SHOOT, 0, (context) -> {
            SingleAnimationSequence animation = shoot;
            ReadOnlyTag states = context.getStates();
            boolean empty = view.getAmmoLeft(states) == 0;
            if (view.stuck(states)) {
                animation = empty ? shootLastStuck : shootStuck;
            } else if (empty) {
                animation = shootLast;
            }
            getTrack("shoot").play(animation.prepare());
        });

    }

    @Override
    public void initAnimation(AKModel model) {
        registerAnimations(
                "shoot", "gcr:ak74m_shoot",
                "shoot_last", "gcr:ak74m_shoot_last",
                "shoot_stuck", "gcr:ak74m_shoot_stuck",
                "shoot_last_stuck", "gcr:ak74m_shoot_last_stuck"
        );
    }

    @Override
    public void customThirdPersonAnimation(AKModel model, ModuleRenderContext context) {
        long startTime = GunEffectManager.getEffectTimestamp(
                context.entity.getId(),
                GunEffect.SHOOT,
                context.currentRenderNode().id
        );
        if (startTime != -1) {
            KeyframeAnimator.animate(model, thirdPersonShoot, startTime, 0.9f);
        }
    }

    @Override
    public void initTrack(AKModel moduleModel) {
        defineTrack("main").addOnPlayed(instance -> getTrack("check").clear());
        defineTrack("shoot").addOnPlayed(instance -> getTrack("check").clear());

        defineTrack("draw").addOnPlayed(instance -> getTrack("check").clear());

        defineTrack("check").addOnApplied((ctx, model) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack("check").clear();
            }
        });
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof AKModel;
    }
}
