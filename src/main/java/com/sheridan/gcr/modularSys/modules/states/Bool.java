package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class Bool extends State<Boolean>{
    public Bool(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public Bool(String name) {
        super(name, false);
    }


    @Override
    public Boolean read(@NotNull CompoundTag states) {
        return states.getBoolean(name);
    }

    @Override
    public void write(@NotNull Boolean value, @NotNull CompoundTag states) {
        states.putBoolean(name, value);
    }
}
