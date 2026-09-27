package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.modules.IHeatSensitiveModular;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class ARBarrel extends SlotProviderModule implements IVoxelHandlerModule, IHeatSensitiveModular {
    private final IVoxelHandler voxelHandler;
    private final float heatSensitive;

    public ARBarrel(ResourceLocation id, float weight, float spreadDec, float heatSensitive, float rpmInc, float impulseInc, float stabilityInc, float damageInc,
                    ISlotProvider slotProvider, IVoxelHandler voxelHandler) {
        super(id, weight, true, Direction.NONE, slotProvider);
        this.voxelHandler = voxelHandler;
        defPropDec(BaseProperties.class, (p) -> p.spread, spreadDec);
        this.heatSensitive = Math.max(heatSensitive, 0.1f);
        defPropInc(BaseProperties.class, p -> p.rpm, rpmInc);
        defPropInc(BaseProperties.class, p -> p.impulse, impulseInc);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
        defPropInc(BaseProperties.class, p -> p.baseDamage, damageInc);

    }

    //中性
    public ARBarrel(ResourceLocation id, float weight, float spreadDec, float heatSensitive,
                    ISlotProvider slotProvider, IVoxelHandler voxelHandler) {
        this(id, weight, spreadDec, heatSensitive, 0, 0, 0, 0, slotProvider, voxelHandler);
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }

    @Override
    public float getHeatSensitive() {
        return heatSensitive;
    }
}
