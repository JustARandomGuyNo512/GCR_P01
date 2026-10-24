package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlotProviderVoxelModule extends AttachmentModule implements ISlotProviderModular, IVoxelHandlerModule{
    private ISlotProvider slotProvider;
    private IVoxelHandler handler;
    public SlotProviderVoxelModule(ResourceLocation id, boolean fixedPosition, float weight, Direction direction, ISlotProvider slotProvider, IVoxelHandler handler) {
        super(id, fixedPosition, weight, direction);
        this.slotProvider = slotProvider;
        this.handler = handler;
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

    @Override
    public IVoxelHandler getHandler() {
        return handler;
    }
}
