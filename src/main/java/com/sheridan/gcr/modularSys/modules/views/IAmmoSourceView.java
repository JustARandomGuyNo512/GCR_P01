package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface IAmmoSourceView extends IStateView{
    int NONE_PRIORITY = -1;
    int GUN_BASE_PRIORITY = 0;
    int COMMON_MAG_PRIORITY = 1;
    int HIGH_PRIORITY = 2;

    int getAmmoLeft(CompoundTag states);
    int getMaxCapacity();
    int getPriority();
}
