package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface IScopeView extends IStateView{
    float getMaxRate();
    float getMinRate();
    float getRate(CompoundTag states);
    float getRatio(CompoundTag states);
}
