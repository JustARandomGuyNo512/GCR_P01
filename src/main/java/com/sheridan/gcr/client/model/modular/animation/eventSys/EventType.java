package com.sheridan.gcr.client.model.modular.animation.eventSys;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EventType {
    /**
     * {@link #SHOOT} 事件参数：这一发是否卡壳。
     *
     * <p>卡壳由开火线程在扣弹时判定。开火线程与渲染线程不是同一个线程，而物品 states 是
     * {@code CompoundTag}（底层 HashMap），渲染线程回读可能滞后、甚至读到写入前的旧值——
     * 于是 {@code shoot_stuck} 选不中。判定结果随事件参数直接带过来，就不需要再回读 states。</p>
     */
    public static final String PARAM_STUCK = "stuck";

    /**
     * {@link #SHOOT} 事件参数：这一发是否是弹匣打空前的最后一发（打完后膛内为空且装着弹匣）。
     * 与 {@link #PARAM_STUCK} 同理，由开火线程在扣弹后算好直接下发。
     */
    public static final String PARAM_LAST_ROUND = "last_round";

    public static final EventType SHOOT = new EventType(true, "shoot");
    public static final EventType SWITCH_FIRE_MODE = new EventType(true, "switch_fire_mode");
    public static final EventType RELOAD = new EventType(true, "reload");
    public static final EventType CHECK_MAG = new EventType(true, "check_mag");
    public static final EventType CHECK_CHAMBER = new EventType(true, "check_chamber");
    public static final EventType REMOVE_STUCK = new EventType(true, "remove_stuck");
    public static final EventType CHECK_SUB_WEAPON = new EventType(true, "check_sub_weapon");
    public static final EventType RELOAD_SUB_WEAPON = new EventType(true, "reload_sub_weapon");
    public static final EventType CLEAR_TRACK = new EventType(true, "clear_track");


    public static final EventType DRAW = new EventType(false, "draw");
    public static final EventType HOLSTER = new EventType(false, "holster");


    public boolean dispatchPerNodeInstance;
    public String name;

    public EventType(boolean dispatchPerNodeInstance, String name) {
        this.dispatchPerNodeInstance = dispatchPerNodeInstance;
        this.name = name;
    }
}
