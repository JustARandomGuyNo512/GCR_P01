package com.sheridan.gcr.modularSys;

import java.util.HashMap;
import java.util.Map;

public class MultiVoxel {
    public final Map<String, IVoxel> voxels;

    public MultiVoxel() {
        this.voxels = new HashMap<>();
    }

    public void setVoxelIfNull(String index, IVoxel voxel) {
        voxels.putIfAbsent(index, voxel);
    }

    public IVoxel getVoxel(String index) {
        return voxels.get(index);
    }

    public IVoxel getVoxelOrThrow(String index) {
        IVoxel voxel = getVoxel(index);
        if (voxel == null) {
            throw new IllegalArgumentException("voxel index: " + index + " does not exist");
        }
        return voxel;
    }
}
