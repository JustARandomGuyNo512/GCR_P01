package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class LInt extends State<Long>{
    public LInt(String name, long defaultValue) {
        super(name, defaultValue);
    }

    public LInt(String name) {
        this(name, 0);
    }

    @Override
    public Long read(@NotNull CompoundTag states) {
        return states.getLong(name);
    }

    @Override
    public void write(@NotNull Long value, @NotNull CompoundTag states) {
        states.putLong(name, value);
    }
}
