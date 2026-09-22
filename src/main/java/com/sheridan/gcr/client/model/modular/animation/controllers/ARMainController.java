package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.ARMainModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.ARView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ARMainController extends GunController<ARMainModel> {
    private Consumer<ARMainController> animationRegister;

    public ARMainController(Consumer<ARMainController> animationRegister) {
        this.animationRegister = animationRegister;
    }

    @Override
    public void firstPersonSubscriptions(ARMainModel model) {
        super.firstPersonSubscriptions(model);
        ARView view = model.getView();

        subscribe(EventType.SHOOT, 0, (context) -> {
            String clip = "shoot";
            ReadOnlyTag states = context.getStates();
            if (view.stuck(states)) {
                clip = "shoot_stuck";
            } else if (view.getAmmoLeft(states) == 0 && view.hasMagAttachment(states)) {
                clip = "shoot_last";
            }
            SHOOT.play(anim(clip).coverState());
        });


        subscribe(EventType.CHECK_CHAMBER, 0, (context) -> {
            ReadOnlyTag states = context.getStates();
            if (isTrackClear(MAIN)) {
                if (view.stuck(states) || view.boltLocked(states)) {
                    CHECK.play(anim("check_chamber_simple"));
                } else {
                    CHECK.play(anim("check_chamber").coverStateExclude("ammo"));
                }
            }
        });

    }

    @Override
    public void customThirdPersonAnimation(ARMainModel model, ModuleRenderContext context) {
        long startTime = GunEffectManager.getEffectTimestamp(
                context.entity.getId(),
                GunEffect.SHOOT,
                context.currentRenderNode().id
        );
        if (startTime != -1) {
            AnimationDef shoot = animDef("shoot", startTime);
            if (shoot != null) {
                KeyframeAnimator.animate(model, shoot, startTime, 0.9f);
            }
        }
    }

    @Override
    public void initAnimation(ARMainModel model) {
        animationRegister.accept(this);
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof ARMainModel;
    }


}
