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

public class CantedRail extends SlotProviderVoxelModule {
    public CantedRail(ResourceLocation id, IVoxelHandler voxelHandler, ISlotProvider provider, float weight) {
        super(id, false, weight, Direction.UPPER, provider, voxelHandler);
    }


    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        List<Unit> sights = accessor.getSlotChildren(thisUnit, "SIGHT");
        for (Unit sight : sights) {
            accessor.writeCustomParam(sight, ISight.ON_SIDE_POSITION, 1);
        }
    }
}