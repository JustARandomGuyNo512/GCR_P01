package com.sheridan.gcr.modularSys;

import net.minecraft.world.phys.AABB;

import java.util.List;

public interface IVoxel {
    List<AABB> getInnerShape();

    AABB getBoundingBox();

    IVoxel copy();
}
