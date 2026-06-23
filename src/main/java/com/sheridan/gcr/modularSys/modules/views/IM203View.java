package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface IM203View extends IStateView{
    String getChamberStatus(CompoundTag states);
}
