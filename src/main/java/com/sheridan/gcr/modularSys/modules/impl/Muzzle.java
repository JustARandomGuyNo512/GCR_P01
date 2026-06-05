package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class Muzzle extends AttachmentModule {
    public Muzzle(ResourceLocation id, float weight, float impulseDec, float stabilityInc) {
        super(id, true, weight, Direction.NONE);
        defPropDec(BaseProperties.class, p -> p.impulse, impulseDec);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
    }

}
