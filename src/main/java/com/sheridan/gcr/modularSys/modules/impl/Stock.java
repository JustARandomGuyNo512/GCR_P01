package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class Stock extends AttachmentModule {

    public Stock(ResourceLocation id, float weight, float recoilControlInc, float stabilityInc) {
        super(id, true, weight, Direction.NONE);
        defPropInc(BaseProperties.class, p -> p.recoilControl, recoilControlInc);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
    }

}
