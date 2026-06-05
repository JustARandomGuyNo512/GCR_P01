package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class Int extends State<Integer>{
    public Int(String name, int defaultValue) {
        super(name, defaultValue);
    }

    public Int(String name) {
        this(name, 0);
    }

    @Override
    public Integer read(@NotNull CompoundTag states) {
        return states.getInt(name);
    }

    @Override
    public void write(@NotNull Integer value, @NotNull CompoundTag states) {
        states.putInt(name, value);
    }
}
