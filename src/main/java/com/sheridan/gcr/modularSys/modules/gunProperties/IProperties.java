package com.sheridan.gcr.modularSys.modules.gunProperties;

import net.minecraft.nbt.CompoundTag;

public interface IProperties {

    void bindProp(CompoundTag bindProp);

    void unbindProp();

    boolean hasProp(String name);

    IProp getProp(String name);

    <T extends IProp> T getProp(String name, Class<T> clazz);

    CompoundTag genInitialTag();

    void inc(IProp prop, float inc);
    void dec(IProp prop, float dec);

    String getId();
}
