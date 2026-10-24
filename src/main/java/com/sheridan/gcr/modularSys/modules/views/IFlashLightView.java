package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface IFlashLightView  extends IStateView{
    boolean isOn(CompoundTag states);

    float getLuminance();

    float getRange();

    float getAngle();
}
