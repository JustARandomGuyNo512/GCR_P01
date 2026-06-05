package com.sheridan.gcr.client.model.playerArm.builder;

import com.sheridan.gcr.client.model.playerArm.GModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GLayerDefinition {
    private final GMeshDefinition mesh;
    private final GMaterialDefinition material;

    private GLayerDefinition(GMeshDefinition mesh, GMaterialDefinition material) {
        this.mesh = mesh;
        this.material = material;
    }

    public GModelPart bakeRoot() {
        return this.mesh.getRoot().bake(this.material.xTexSize, this.material.yTexSize);
    }

    public static GLayerDefinition create(GMeshDefinition mesh, int texWidth, int texHeight) {
        return new GLayerDefinition(mesh, new GMaterialDefinition(texWidth, texHeight));
    }
}
