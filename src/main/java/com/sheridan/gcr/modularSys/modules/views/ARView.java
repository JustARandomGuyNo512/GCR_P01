package com.sheridan.gcr.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface ARView extends IGunView {
    boolean boltLocked(CompoundTag states);
}
