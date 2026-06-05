package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class Num extends State<Float>{
    public Num(String name, float defaultValue) {
        super(name, defaultValue);
    }

    public Num(String name) {
        super(name, 0.0f);
    }


    @Override
    public Float read(@NotNull CompoundTag states) {
        return states.getFloat(name);
    }

    @Override
    public void write(@NotNull Float value, @NotNull CompoundTag states) {
        states.putFloat(name, value);
    }
}
