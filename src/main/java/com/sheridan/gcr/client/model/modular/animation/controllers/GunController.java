package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.animation.eventSys.Track;

public abstract class  GunController<T extends IModularModel>  extends AnimationController<T> {
    protected Track<T> MAIN;
    protected Track<T> SHOOT;
    protected Track<T> DRAW;
    protected Track<T> CHECK;

    @Override
    public void firstPersonSubscriptions(T model) {
        super.firstPersonSubscriptions(model);
        subscribe(EventType.DRAW, 0, (context) -> {
            if (isTrackClear("main")) {
                System.out.println("dispatch2");
                DRAW.play(anim("draw"));
            }
        });

        subscribe(EventType.HOLSTER, 0, (context) -> {
            if (isTrackClear("main")) {
                DRAW.play(
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
    public void initTrack(T moduleModel) {
        MAIN = defineTrack("main").addOnPlayed(instance -> getTrack("check").clear());
        SHOOT = defineTrack("shoot").addOnPlayed(instance -> getTrack("check").clear());

        DRAW = defineTrack("draw").addOnPlayed(instance -> getTrack("check").clear());

        CHECK = defineTrack("check").addOnApplied((ctx, model) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack("check").clear();
            }
        });
    }
}
