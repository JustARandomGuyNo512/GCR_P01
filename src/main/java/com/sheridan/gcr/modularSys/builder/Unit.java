package com.sheridan.gcr.modularSys.builder;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Unit {
    public static final String NONE = "__none__";
    public static final String SLOT_NAME = "slot";
    public static final String MODULE_ID = "module";
    public static final String IN_TIME_ID = "id";
    public static final String PARENT_ID = "parent";
    public static final String OFFSET_Z = "z";
    public static final String RENDER_PARAMS = "render_params";
    public static final String REVERSE_MODEL = "reverse_model";
    private float zOffset = 0.0f;
    private final IModular module;
    private final Map<String, Integer> renderParams = new HashMap<>();

    public Unit(@NotNull IModular module) {
        Objects.requireNonNull(module);
        this.module = module;
    }

    public static Unit of(IModular modular) {
        return new Unit(modular);
    }

    @Nullable
    public static Unit of(CompoundTag tag) {
        IModular iModular = ModuleRegister.get(tag.getString(Unit.MODULE_ID));
        if (iModular == null) {
            return null;
        }
        Unit unit = Unit.of(iModular);
        unit.read(tag);
        return unit;
    }

    public IModular getModule() {
        return module;
    }

    public boolean hasTag(String tag) {
        return module.hasTag(tag);
    }

    public String getModuleId() {
        return module.getID();
    }

    public float getZOffset() {
        return zOffset;
    }

    void setZOffset(float zOffset) {
        this.zOffset = zOffset;
    }

    void setRenderParams(String key, int value) {
        if (value == -1) {
            renderParams.remove(key);
            return;
        }
        renderParams.put(key, value);
    }

    void flush() {
        renderParams.clear();
    }

    Map<String, Integer> getCustomParams() {
        return renderParams;
    }

    public int getCustomParam(String key) {
        Integer i = renderParams.get(key);
        return i == null ? -1 : i;
    }

    Unit copy() {
        Unit unit = Unit.of(module);
        unit.zOffset = zOffset;
        unit.renderParams.putAll(renderParams);
        return unit;
    }

    Unit copyFrom(Unit unit) {
        this.zOffset = unit.zOffset;
        this.renderParams.putAll(unit.renderParams);
        return this;
    }

    void read(CompoundTag tag) {
        zOffset = tag.getFloat(Unit.OFFSET_Z);
        if (tag.contains(Unit.RENDER_PARAMS)) {
            CompoundTag customTag = tag.getCompound(Unit.RENDER_PARAMS);
            for (String key : customTag.getAllKeys()) {
                renderParams.put(key, customTag.getInt(key));
            }
        }
    }

    void write(CompoundTag tag) {
        tag.putString(Unit.MODULE_ID, module.getID());
        tag.putFloat(Unit.OFFSET_Z, zOffset);
        if (!renderParams.isEmpty()) {
            CompoundTag customTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : renderParams.entrySet()) {
                if (entry.getValue() == -1) {
                    continue;
                }
                customTag.putInt(entry.getKey(), entry.getValue());
            }
            tag.put(Unit.RENDER_PARAMS, customTag);
        }
    }

    public void print() {
        System.out.println("{module id: " +  module.getID() + " zOffset: " + this.zOffset + "}");
        if (!renderParams.isEmpty()) {
            for (Map.Entry<String, Integer> entry : renderParams.entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
        System.out.println();
    }

    boolean diff(Unit unit) {
        if (unit == this) {
            return false;
        }
        return unit.getModule() != this.module ||
                unit.zOffset != this.zOffset;
    }

    @Override
    public String toString() {
        return getModuleId();
    }

    public Direction getDirection() {
        return module.getDirection();
    }
}
