package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IGunModel {
    String DEFAULT_FARTHEST_SIGHT_Z_NAME = "FARTHEST_SIGHT_Z";

    String getFarthestSightZName(ModuleRenderContext context);
}
