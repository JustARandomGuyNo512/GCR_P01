package com.sheridan.gcr.modularSys.modules.states;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class State<T> {
    public final String name;
    private final T defaultValue;

    public State(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }


    public T getDefaultValue() {
        return defaultValue;
    }

    public void init(CompoundTag states) {
        if (states != null) {
            set(getDefaultValue(), states);
        }
    }

    public String getName() {
        return name;
    }

    public T get(@Nullable CompoundTag states) {
        if (states == null) {
            return getDefaultValue();
        }
        return states.contains(name) ? read(states) : getDefaultValue();
    }

    public void set(@Nullable T value, @Nullable CompoundTag states) {
        if (states != null && value != null) {
            write(value, states);
        }
    }

    public void encode(@NotNull CompoundTag from, @NotNull CompoundTag payLoad) {
        T t = get(from);
        write(t, payLoad);
    }

    public void decode(@NotNull CompoundTag payLoad, @NotNull CompoundTag to) {
        T t = read(payLoad);
        write(t, to);
    }

    protected abstract T read(@NotNull CompoundTag states);
    protected abstract void write(@NotNull T value, @NotNull CompoundTag states);
}
