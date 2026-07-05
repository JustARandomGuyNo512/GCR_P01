package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.MultiVoxel;
import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.impl.SplitARHandguard;
import net.minecraft.resources.ResourceLocation;

public class SplitARHandguardVoxelHandler extends VoxelHandler {
    private IVoxel total;
    private IVoxel upper;

    public SplitARHandguardVoxelHandler(ResourceLocation voxelAssetPath) {
        super(voxelAssetPath);
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {
        super.setVoxelIfNull(voxel);
        total = voxel.getVoxelOrThrow("total");
        upper = voxel.getVoxelOrThrow("upper");
    }

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        int customParam = unit.getCustomParam(SplitARHandguard.HIDE_LOWER_PART_PARAM_KEY);
        return customParam == -1 ? total : upper;
    }
}
