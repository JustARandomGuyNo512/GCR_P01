package com.sheridan.gcr;

import net.minecraft.nbt.CompoundTag;

public interface INBTSync {
    void writeData(CompoundTag tag);
    void loadData(CompoundTag tag);
}
