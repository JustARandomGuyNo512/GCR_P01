package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface IStateView {
    String getNodeId(CompoundTag states);
    String getModuleId(CompoundTag states);
}
