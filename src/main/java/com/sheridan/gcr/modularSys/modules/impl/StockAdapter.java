package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class StockAdapter extends Stock implements ISlotProviderModular {
    private ISlotProvider slotProvider;

    public StockAdapter(ResourceLocation id, float weight, ISlotProvider slotProvider) {
        super(id, weight, 0, 0);
        this.slotProvider = slotProvider;
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }
}
