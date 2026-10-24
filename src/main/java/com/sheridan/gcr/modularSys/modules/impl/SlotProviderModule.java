package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlotProviderModule extends AttachmentModule implements ISlotProviderModular {
    private final ISlotProvider slotProvider;

    public SlotProviderModule(ResourceLocation id, float weight, boolean fixedPosition, Direction direction, ISlotProvider slotProvider) {
        super(id, fixedPosition, weight, direction);
        this.slotProvider = slotProvider;
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

}
