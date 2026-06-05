package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class ARBarrel extends SlotProviderModule implements IVoxelHandlerModule {
    private final IVoxelHandler voxelHandler;

    public ARBarrel(ResourceLocation id, float weight, float spreadDec, ISlotProvider slotProvider, IVoxelHandler voxelHandler) {
        super(id, weight, true, Direction.NONE, slotProvider);
        this.voxelHandler = voxelHandler;
        defPropDec(BaseProperties.class, (p) -> p.spread, spreadDec);
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }
}
