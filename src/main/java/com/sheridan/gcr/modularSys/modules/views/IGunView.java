package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface IGunView extends IAmmoSourceView {
    boolean stuck(CompoundTag states);
    String getFireModeId(CompoundTag states);
    boolean hasMagAttachment(CompoundTag states);
    float getHeat(CompoundTag states);
    long getHeatLastUpdate(CompoundTag states);
    //game time
    long getLastShootTime(CompoundTag states);
}
