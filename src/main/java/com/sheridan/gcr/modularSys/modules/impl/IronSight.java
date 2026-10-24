package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import net.minecraft.resources.ResourceLocation;

public class IronSight extends Sight {
    public IronSight(ResourceLocation id, IVoxelHandler voxelHandler, float weight, boolean fixedPosition) {
        super(id, voxelHandler, weight, fixedPosition, 1.0f);
    }

    @Override
    public int getSightPriority(Unit unit) {
        return super.getSightPriority(unit);
    }

    @Override
    public int defaultSightPriority(Unit unit) {
        return IRON_SIGHT;
    }
}
