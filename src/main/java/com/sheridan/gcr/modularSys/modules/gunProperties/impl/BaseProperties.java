package com.sheridan.gcr.modularSys.modules.gunProperties.impl;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.modules.gunProperties.AccProp;
import com.sheridan.gcr.modularSys.modules.gunProperties.IntProp;
import com.sheridan.gcr.modularSys.modules.gunProperties.NumProp;
import com.sheridan.gcr.modularSys.modules.gunProperties.Properties;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class BaseProperties extends Properties {
    public static final String ID = GCR.RL("base").toString();

    public final IntProp rpm;
    //垂直压强
    public final NumProp recoilControl;
    //水平、随机振动，射击稳定性
    public final NumProp stability;
    public final AccProp weight;
    public final NumProp spread;
    public final NumProp faultRate;
    public final NumProp agility;
    public final NumProp impulse;
    public final NumProp aimingSpeed;

    public Map<String, Float> taskTimers;

    public BaseProperties(int rpm,  float weight, float spread, float agility, float faultRate, float aimingSpeed, Map<String, Float> taskTimers) {
        super(GCR.RL("base"));
        this.rpm = defProp(new IntProp("rpm", rpm, 0.8f, 1.2f));
        this.recoilControl = defProp(new NumProp("recoil_control", 1));
        this.stability = defProp(new NumProp("stability", 1));
        this.weight = defProp(new AccProp("weight", weight));
        this.spread = defProp(new NumProp("spread", spread));
        this.agility = defProp(new NumProp("agility", agility));
        this.faultRate = defProp(new NumProp("fault_rate", faultRate));
        this.impulse = defProp(new NumProp("impulse", 1));
        this.aimingSpeed = defProp(new NumProp("aiming_speed", aimingSpeed));
        this.taskTimers = taskTimers;
    }

    public CompoundTag pick(ItemStack itemStack, IGun gun) {
        CompoundTag propertiesTag = gun.getPropertiesTag(itemStack);
        return propertiesTag.getCompound(getId());
    }
}
