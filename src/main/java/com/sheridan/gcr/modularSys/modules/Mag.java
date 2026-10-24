package com.sheridan.gcr.modularSys.modules;

import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.impl.AttachmentModule;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Mag extends AttachmentModule implements IAmmoSource {
    private final int maxCapacity;

    public Mag(ResourceLocation id, float weight, int maxCapacity) {
        super(id, true, weight, Direction.NONE);
        this.maxCapacity = maxCapacity;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public void setAmmoLeft(int ammoLeft, CompoundTag states) {
        ammoLeft = Mth.clamp(ammoLeft, 0, maxCapacity);
        AMMO_LEFT.set(ammoLeft, states);
    }

    @Override
    public int getAmmoLeft(CompoundTag states) {
        return AMMO_LEFT.get(states);
    }

    @Override
    public int getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public int getPriority() {
        return IAmmoSourceView.COMMON_MAG_PRIORITY;
    }

}
