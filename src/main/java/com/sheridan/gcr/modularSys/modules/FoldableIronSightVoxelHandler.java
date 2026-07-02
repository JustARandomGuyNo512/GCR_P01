package com.sheridan.gcr.modularSys.modules;

import net.minecraft.resources.ResourceLocation;

public class FoldableIronSightVoxelHandler extends VoxelHandler {
    public FoldableIronSightVoxelHandler(ResourceLocation voxelAssetPath, boolean ignoreBoundaryCollisionRear, boolean ignoreBoundaryCollisionFar) {
        super(voxelAssetPath, ignoreBoundaryCollisionRear, ignoreBoundaryCollisionFar);
    }
}
