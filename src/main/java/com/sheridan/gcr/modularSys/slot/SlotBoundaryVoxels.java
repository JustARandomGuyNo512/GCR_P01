package com.sheridan.gcr.modularSys.slot;

import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.Voxel;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SlotBoundaryVoxels {
    private static final Voxel REAR_VOXEL;
    private static final Voxel FAR_VOXEL;

    static {
        REAR_VOXEL = new Voxel(List.of(new AABB(
                -0.05624999850988388f, -0.01875000074505806f, 0.0f,
                0.05624999850988388f, 0.01875000074505806f, 6.25f)));

        FAR_VOXEL = new Voxel(List.of(new AABB(
                -0.05624999850988388f, -0.01875000074505806f, -6.25f,
                0.05624999850988388f, 0.01875000074505806f, 0.0f)));

    }

    public static IVoxel getRearVoxel() {
        return REAR_VOXEL;
    }

    public static IVoxel getFarVoxel() {
        return FAR_VOXEL;
    }
}
