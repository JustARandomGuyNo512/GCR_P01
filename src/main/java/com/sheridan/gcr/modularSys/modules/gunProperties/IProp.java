package com.sheridan.gcr.modularSys.modules.gunProperties;

import com.sheridan.gcr.IJsonSync;
import com.sheridan.gcr.INBTSync;
import net.minecraft.nbt.CompoundTag;

public interface IProp extends IJsonSync, INBTSync {
    void inc(CompoundTag tag, float incRate);

    void dec(CompoundTag tag, float decRate);

    float get(CompoundTag properties);

    float getRatio(CompoundTag properties);

    String getKey();

    void setFullName(String fullName);

    String getFullNameKey();

    String getFullName();

    void setUpDefault(CompoundTag tag);
}
