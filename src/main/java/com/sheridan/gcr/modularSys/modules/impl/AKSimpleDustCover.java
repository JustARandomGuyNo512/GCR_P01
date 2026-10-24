package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class AKSimpleDustCover extends AttachmentModule {
    public AKSimpleDustCover(ResourceLocation id, float weight, float minStuckRateDec, float maxStuckRateDec) {
        super(id, true, weight, Direction.NONE);
        defPropDec(BaseProperties.class, p -> p.stuckRate, minStuckRateDec);
        defPropDec(BaseProperties.class, p -> p.maxStuckRate, maxStuckRateDec);
    }
}
