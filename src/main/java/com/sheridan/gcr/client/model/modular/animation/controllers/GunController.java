package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.Callback;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.animation.eventSys.Track;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.modularSys.modules.views.IGunView;

public abstract class  GunController<T extends IModularModel>  extends AnimationController<T> {
    protected Track<T> MAIN;
    protected Track<T> SHOOT;
    protected Track<T> DRAW;
    protected Track<T> CHECK;

    @Override
    public void firstPersonSubscriptions(T model) {
        super.firstPersonSubscriptions(model);
        subscribe(EventType.DRAW, 0, (context) -> {
            if (isTrackClear(MAIN)) {
                DRAW.play(anim("draw"));
            }
        });

        subscribe(EventType.HOLSTER, 0, (context) -> {
            if (isTrackClear(MAIN)) {
                DRAW.play(anim("holster").keepOnLastFrame());
            }
        });

        subscribe(EventType.RELOAD, 0, (context) -> {
            String name = context.getParam("animation_name");
            MAIN.play(anim(name).coverState());
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 10, (context) -> {
            String name = context.getParam("animation_name");
            MAIN.play(anim(name).coverState());
        });

        subscribe(EventType.CHECK_MAG, 0, (context) -> {
            if (isTrackClear(MAIN)) {
                CHECK.play(anim("check_mag").coverState());
            }
        });

        subscribe(EventType.CHECK_SUB_WEAPON, 10, (context) -> {
            if (isTrackClear(MAIN)) {
                String animationName = context.getParam("animation_name");
                CHECK.play(anim(animationName));
            } else {
                context.cancel();
            }
        });

        subscribe(EventType.REMOVE_STUCK, 0, (context) -> {
            String name = context.getParam("name");
            MAIN.play(anim(name).coverState());
        });

        subscribe(EventType.SWITCH_FIRE_MODE, 0, (context) -> {
            String after = context.getParam("after");
            MAIN.play(anim(after).coverState());
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

    /**
     * 读 {@link EventType#SHOOT} 的卡壳参数。
     *
     * <p>开火线程（{@code ClientWeaponLooper}）与渲染线程不是同一个线程，物品 states 是
     * {@code CompoundTag}（底层 HashMap），渲染线程回读可能滞后甚至读到写入前的旧值。
     * 因此开火线程会把这一发的判定结果直接塞进事件参数；只有参数缺失（其它派发方）时，
     * 才退回读 states 以保持兼容。</p>
     */
    protected static boolean isShootStuck(Callback.EventContext context, IGunView view) {
        String param = context.getParam(EventType.PARAM_STUCK);
        if (param != null) {
            return Boolean.parseBoolean(param);
        }
        return view.stuck(context.getStates());
    }

    /**
     * 读 {@link EventType#SHOOT} 的「最后一发」参数：打完后膛内为空、且这把枪装着弹匣。
     * 与 {@link #isShootStuck} 同理，优先用开火线程算好的参数，缺失时退回读 states。
     */
    protected static boolean isShootLastRound(Callback.EventContext context, IGunView view) {
        String param = context.getParam(EventType.PARAM_LAST_ROUND);
        if (param != null) {
            return Boolean.parseBoolean(param);
        }
        ReadOnlyTag states = context.getStates();
        return view.getAmmoLeft(states) == 0 && view.hasMagAttachment(states);
    }
}
