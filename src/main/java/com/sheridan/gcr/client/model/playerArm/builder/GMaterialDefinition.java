package com.sheridan.gcr.client.model.playerArm.builder;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GMaterialDefinition {
    final int xTexSize;
    final int yTexSize;

    public GMaterialDefinition(int xTexSize, int yTexSize) {
        this.xTexSize = xTexSize;
        this.yTexSize = yTexSize;
    }
}