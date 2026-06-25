package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.animation.SingleAnimationSequence;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.ARMainModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARFullAuto;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARSemi;
import com.sheridan.gcr.modularSys.modules.views.ARView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ARMainController extends AnimationController<ARMainModel> {
    private SingleAnimationSequence shoot;
    private SingleAnimationSequence shootLast;
    private SingleAnimationSequence shootStuck;

    private AnimationDef thirdPersonShoot;


    @Override
    public void firstPersonSubscriptions(ARMainModel model) {
        ARView view = model.getView();

        shoot = new SingleAnimationSequence(anim("shoot").coverState());
        shootLast = new SingleAnimationSequence(anim("shoot_last").coverState());
        shootStuck = new SingleAnimationSequence(anim("shoot_stuck").coverState());

        thirdPersonShoot = anim("shoot").animation;

        subscribe(EventType.SHOOT, 0, (context) -> {
            SingleAnimationSequence animation = shoot;
            ReadOnlyTag states = context.getStates();
            if (view.stuck(states)) {
                animation = shootStuck;
            } else if (view.getAmmoLeft(states) == 0 && view.hasMagAttachment(states)) {
                animation = shootLast;
            }
            getTrack("shoot").play(animation.prepare());
        });

        subscribe(EventType.SWITCH_FIRE_MODE, 0, (context) -> {
            String after = context.getParam("after");
            getTrack("main").play(anim(after).coverState());
        });
        
        subscribe(EventType.RELOAD, 0, (context) -> {
            String name = context.getParam("animation_name");
            getTrack("main").play(anim(name).coverState());
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 0, (context) -> {
            String name = context.getParam("animation_name");
            getTrack("main").play(anim(name).coverState());
        });

        subscribe(EventType.CHECK_MAG, 0, (context) -> {
            if (isTrackClear("main")) {
                getTrack("check").play(anim("check_mag").coverState());
            }
        });

        subscribe(EventType.CHECK_CHAMBER, 0, (context) -> {
            ReadOnlyTag states = context.getStates();
            if (isTrackClear("main")) {
                if (view.stuck(states) || view.boltLocked(states)) {
                    getTrack("check").play(anim("check_chamber_simple"));
                } else {
                    getTrack("check").play(anim("check_chamber").coverStateExclude("ammo"));
                }
            }
        });

        subscribe(EventType.CHECK_SUB_WEAPON, 0, (context) -> {
            if (isTrackClear("main")) {
                String animationName = context.getParam("animation_name");
                getTrack("check").play(anim(animationName));
            }
        });


        subscribe(EventType.REMOVE_STUCK, 0, (context) -> {
            String name = context.getParam("name");
            getTrack("main").play(anim(name).coverState());
        });

        subscribe(EventType.DRAW, 0, (context) -> {
            if (isTrackClear("main")) {
                getTrack("draw").play(anim("draw"));
            }
        });

        subscribe(EventType.HOLSTER, 0, (context) -> {
            if (isTrackClear("main")) {
                getTrack("draw").play(
                        anim("holster")
                                .keepOnLastFrame()
                                .setOnPlaying((progress) -> {
                                    DrawHolsterHandler.State state = DrawHolsterHandler.get().getState();
                                    if (state != DrawHolsterHandler.State.HOLSTERING) {
                                        clearTrack("draw");
                                    }
                                })
                );
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
            KeyframeAnimator.animate(model, thirdPersonShoot, startTime, 0.9f);
        }
    }

    @Override
    public void initAnimation(ARMainModel model) {
        registerAnimations(
                "check_mag", "gcr:check_mag",
                "reload_grenade", "gcr:reload_grenade",
                "check_grenade", "gcr:check_grenade",
                ARFullAuto.FULL_AUTO.getName(), "gcr:to_auto",
                ARSemi.SEMI.getName(), "gcr:to_semi",
                "mag_reload", "gcr:m4a1_mag_reload",
                "mag_reload_empty", "gcr:m4a1_mag_reload_empty",
                "mag_reload_charge", "gcr:m4a1_mag_reload_charge",
                "chamber_reload", "gcr:m4a1_chamber_reload",
                "chamber_reload_empty", "gcr:m4a1_chamber_reload_empty",
                "check_chamber", "gcr:m4a1_check_chamber",
                "check_chamber_simple", "gcr:m4a1_check_chamber_simple",
                "shoot", "gcr:m4a1_shoot",
                "shoot_last", "gcr:m4a1_shoot_last",
                "shoot_stuck", "gcr:m4a1_shoot_stuck",
                "remove_stuck", "gcr:m4a1_remove_stuck",
                "remove_stuck_empty", "gcr:m4a1_remove_stuck_empty",
                "holster", "gcr:ar_holster",
                "draw", "gcr:ar_draw"
        );
    }

    @Override
    public void initTrack(ARMainModel moduleModel) {
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
        return model instanceof ARMainModel;
    }


}
