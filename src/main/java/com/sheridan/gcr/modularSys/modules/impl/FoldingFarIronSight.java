package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.FoldingIronSightVoxelHandler;
import com.sheridan.gcr.modularSys.modules.ISight;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class FoldingFarIronSight extends AttachmentModule implements IVoxelHandlerModule {
    private final FoldingIronSightVoxelHandler voxelHandler;
    public FoldingFarIronSight(ResourceLocation id, boolean fixedPosition, float weight, FoldingIronSightVoxelHandler voxelHandler) {
        super(id, fixedPosition, weight, Direction.UPPER);
        this.voxelHandler = voxelHandler;
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        FoldingRearIronSight.handleFolding(accessor, thisUnit);
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }
}
