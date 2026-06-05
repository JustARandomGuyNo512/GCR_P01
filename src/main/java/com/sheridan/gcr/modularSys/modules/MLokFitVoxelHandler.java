package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.MultiVoxel;
import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import net.minecraft.resources.ResourceLocation;

public class MLokFitVoxelHandler extends VoxelHandler {
    private IVoxel mLok;
    private IVoxel picatinny;

    public MLokFitVoxelHandler(ResourceLocation voxelAssetPath, boolean ignoreBoundaryCollisionRear, boolean ignoreBoundaryCollisionFar) {
        super(voxelAssetPath, ignoreBoundaryCollisionRear, ignoreBoundaryCollisionFar);
    }

    public MLokFitVoxelHandler(ResourceLocation voxelAssetPath) {
        this(voxelAssetPath, false, false);
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {
        super.setVoxelIfNull(voxel);
        init();
    }

    private void init() {
        mLok = voxel.getVoxel("m_lok");
        picatinny = voxel.getVoxel("picatinny");
        if (picatinny == null) {
            throw new IllegalArgumentException("voxel asset path: " + voxelAssetPath + " does not contain 'picatinny' voxel");
        }
        if (mLok == null) {
            throw new IllegalArgumentException("voxel asset path: " + voxelAssetPath + " does not contain 'm_lok' voxel");
        }
    }

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        int customParam = unit.getCustomParam(VOXEL_INDEX_PARAM_KEY);
        return customParam == 1 ? mLok : picatinny;
    }

}
