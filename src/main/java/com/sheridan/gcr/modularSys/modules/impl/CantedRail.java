package com.sheridan.gcr.modularSys.modules.impl;

import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.ISight;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CantedRail extends AttachmentModule implements IVoxelHandlerModule, ISlotProviderModular {
    private final IVoxelHandler voxelHandler;
    private final ISlotProvider provider;

    public CantedRail(ResourceLocation id, IVoxelHandler voxelHandler, ISlotProvider provider, float weight) {
        super(id, false, weight, Direction.UPPER);
        this.voxelHandler = voxelHandler;
        this.provider = provider;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        List<Unit> sights = accessor.getSlotChildren(thisUnit, "SIGHT");
        for (Unit sight : sights) {
            accessor.writeCustomParam(sight, ISight.ON_SIDE_POSITION, 1);
        }
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return provider;
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }
}