package com.sheridan.gcr.client.model.modular.modules;

import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.*;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SightModel extends ModularModel implements ISightModel {
    public SightModel(MeshModelData root, ResourceLocation name) {
        super(root, name);
        getOrThrow(ISightModel.DEFAULT_BONE_NAME);
    }
}
