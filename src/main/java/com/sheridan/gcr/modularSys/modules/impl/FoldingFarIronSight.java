package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;

public class FoldingFarIronSight extends AttachmentModule implements IVoxelHandlerModule {
    public FoldingFarIronSight(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
    }

    @Override
    public IVoxelHandler getHandler() {
        return null;
    }
}
