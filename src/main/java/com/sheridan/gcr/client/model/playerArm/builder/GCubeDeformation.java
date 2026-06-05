package com.sheridan.gcr.client.model.playerArm.builder;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GCubeDeformation {
    public static final GCubeDeformation NONE = new GCubeDeformation(0.0F);
    final float growX;
    final float growY;
    final float growZ;

    public GCubeDeformation(float growX, float growY, float growZ) {
        this.growX = growX;
        this.growY = growY;
        this.growZ = growZ;
    }

    public GCubeDeformation(float grow) {
        this(grow, grow, grow);
    }

    public GCubeDeformation extend(float grow) {
        return new GCubeDeformation(this.growX + grow, this.growY + grow, this.growZ + grow);
    }

    public GCubeDeformation extend(float growX, float growY, float growZ) {
        return new GCubeDeformation(this.growX + growX, this.growY + growY, this.growZ + growZ);
    }
}