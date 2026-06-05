package com.sheridan.gcr.client.model.playerArm.builder;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GMeshDefinition {
    private final GPartDefinition root;

    public GMeshDefinition() {
        this.root = new GPartDefinition(ImmutableList.of(), PartPose.ZERO);
    }

    public GPartDefinition getRoot() {
        return this.root;
    }
}
