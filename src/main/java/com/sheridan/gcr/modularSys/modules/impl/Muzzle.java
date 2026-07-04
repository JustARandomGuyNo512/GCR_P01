package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class Muzzle extends AttachmentModule {
    private final int fireSoundType;

    public Muzzle(ResourceLocation id, float weight, float impulseDec, float stabilityInc, int fireSoundType, float fireSoundRangeInc) {
        super(id, true, weight, Direction.NONE);
        defPropDec(BaseProperties.class, p -> p.impulse, impulseDec);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
        defPropInc(BaseProperties.class, p -> p.fireSoundRange, fireSoundRangeInc);
        this.fireSoundType = fireSoundType;
    }

    public int getFireSoundType() {
        return fireSoundType;
    }
}
