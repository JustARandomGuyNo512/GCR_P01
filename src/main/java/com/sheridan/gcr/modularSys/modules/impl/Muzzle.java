package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.IHeatSensitiveModular;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class Muzzle extends AttachmentModule implements IHeatSensitiveModular {
    private float heatSSensitive;
    private final int fireSoundType;
    private final boolean isSuppressor;

    public Muzzle(ResourceLocation id, float weight, float impulseDec, float stabilityInc, int fireSoundType, float fireSoundRangeInc, float heatSensitive, boolean isSuppressor) {
        super(id, true, weight, Direction.NONE);
        defPropDec(BaseProperties.class, p -> p.impulse, impulseDec);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
        defPropInc(BaseProperties.class, p -> p.fireSoundRange, fireSoundRangeInc);
        this.fireSoundType = fireSoundType;
        this.heatSSensitive = Math.max(0.1f, heatSensitive);
        this.isSuppressor = isSuppressor;

    }


    public int getFireSoundType() {
        return fireSoundType;
    }

    @Override
    public float getHeatSensitive() {
        return heatSSensitive;
    }

    public boolean isSuppressor() {
        return isSuppressor;
    }
}
