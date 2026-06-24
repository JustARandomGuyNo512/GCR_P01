package com.sheridan.gcr.client.model.modular.animation.eventSys;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EventType {
    public static final EventType SHOOT = new EventType(true, "shoot");
    public static final EventType SWITCH_FIRE_MODE = new EventType(true, "switch_fire_mode");
    public static final EventType RELOAD = new EventType(true, "reload");
    public static final EventType CHECK_MAG = new EventType(true, "check_mag");
    public static final EventType CHECK_CHAMBER = new EventType(true, "check_chamber");
    public static final EventType REMOVE_STUCK = new EventType(true, "remove_stuck");
    public static final EventType CHECK_SUB_WEAPON = new EventType(true, "check_sub_weapon");
    public static final EventType RELOAD_SUB_WEAPON = new EventType(true, "reload_sub_weapon");

    public static final EventType DRAW = new EventType(false, "draw");
    public static final EventType HOLSTER = new EventType(false, "holster");


    public boolean dispatchPerNodeInstance;
    public String name;

    public EventType(boolean dispatchPerNodeInstance, String name) {
        this.dispatchPerNodeInstance = dispatchPerNodeInstance;
        this.name = name;
    }
}
