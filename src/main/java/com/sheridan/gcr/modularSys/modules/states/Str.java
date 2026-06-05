package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class Str extends State<String>{
    public Str(String name, String defaultValue) {
        super(name, defaultValue);
    }

    public Str(String name) {
        super(name, "");
    }

    @Override
    public String read(@NotNull CompoundTag states) {
        return states.getString(name);
    }

    @Override
    public void write(@NotNull String value, @NotNull CompoundTag states) {
        states.putString(name, value);
    }

}
