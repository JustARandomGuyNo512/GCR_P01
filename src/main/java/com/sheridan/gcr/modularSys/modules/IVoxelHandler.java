package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.MultiVoxel;
import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

public interface IVoxelHandler {
    String VOXEL_INDEX_PARAM_KEY = "voxel_index";

    IVoxel getVoxel(Unit unit, Accessor accessor);
    void setVoxelIfNull(MultiVoxel voxel);
    ResourceLocation getAssetPath();
    Pair<Boolean, Boolean> ignoreBoundaryCollision();
}
