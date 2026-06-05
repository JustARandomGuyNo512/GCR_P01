package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.MultiVoxel;
import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

public class VoxelHandler implements IVoxelHandler {
    protected MultiVoxel voxel;
    protected ResourceLocation voxelAssetPath;
    protected Pair<Boolean, Boolean> ignoreBoundaryCollision;

    public VoxelHandler(ResourceLocation voxelAssetPath, boolean ignoreBoundaryCollisionRear, boolean ignoreBoundaryCollisionFar) {
        this.voxelAssetPath = voxelAssetPath;
        this.ignoreBoundaryCollision = Pair.of(ignoreBoundaryCollisionRear, ignoreBoundaryCollisionFar);
    }

    public VoxelHandler(ResourceLocation voxelAssetPath) {
        this(voxelAssetPath, false, false);
    }

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        return voxel.getVoxel("root");
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {
        this.voxel = voxel;
    }

    @Override
    public ResourceLocation getAssetPath() {
        return voxelAssetPath;
    }

    @Override
    public Pair<Boolean, Boolean> ignoreBoundaryCollision() {
        return ignoreBoundaryCollision;
    }
}
