package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ISightModel {
    String DEFAULT_BONE_NAME = "SIGHT_POSE";
    default String getSightPoseBoneName(ModuleRenderContext context) {
        return DEFAULT_BONE_NAME;
    }
}
