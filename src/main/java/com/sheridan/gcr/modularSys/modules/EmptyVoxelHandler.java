package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.IVoxel;
import com.sheridan.gcr.modularSys.MultiVoxel;
import com.sheridan.gcr.modularSys.Voxel;
import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.slot.ISlot;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class EmptyVoxelHandler implements IVoxelHandler{
    public static final EmptyVoxelHandler INSTANCE = new EmptyVoxelHandler();
    public static final IVoxel EMPTY_VOXEL = new Voxel(List.of());

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        return EMPTY_VOXEL;
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {

    }

    @Override
    public ResourceLocation getAssetPath() {
        return null;
    }

    @Override
    public Pair<Boolean, Boolean> ignoreBoundaryCollision(ISlot slot) {
        return Pair.of(true, true);
    }
}
