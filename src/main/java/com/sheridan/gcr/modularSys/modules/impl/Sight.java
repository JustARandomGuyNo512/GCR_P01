package com.sheridan.gcr.modularSys.modules.impl;

import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.ISight;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;

public abstract class Sight extends AttachmentModule implements IVoxelHandlerModule, ISight {
    private final IVoxelHandler voxelHandler;
    private final float adsSpeedModifier;
    public Sight(ResourceLocation id, IVoxelHandler voxelHandler, float weight, boolean fixedPosition, float adsSpeedModifier) {
        super(id, fixedPosition, weight, Direction.UPPER);
        this.voxelHandler = voxelHandler;
        this.adsSpeedModifier = adsSpeedModifier;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }

    @Override
    public float getAdsSpeedModifier() {
        return adsSpeedModifier;
    }
}
