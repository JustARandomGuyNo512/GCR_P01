package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class DNum extends State<Double>{
    public DNum(String name) {
        super(name, 0.0);
    }

    public DNum(String name, double defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public Double read(@NotNull CompoundTag states) {
        return states.getDouble(name);
    }

    @Override
    public void write(@NotNull Double value, @NotNull CompoundTag states) {
        states.putDouble(name, value);
    }
}
