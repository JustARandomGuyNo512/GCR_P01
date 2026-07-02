package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import net.minecraft.resources.ResourceLocation;

public class FoldingRearIronSight extends Sight{
    public FoldingRearIronSight(ResourceLocation id, IVoxelHandler voxelHandler, float weight, boolean fixedPosition, float adsSpeedModifier) {
        super(id, voxelHandler, weight, fixedPosition, adsSpeedModifier);
    }

    @Override
    public int defaultSightPriority(Unit unit) {
        return IRON_SIGHT;
    }
}
