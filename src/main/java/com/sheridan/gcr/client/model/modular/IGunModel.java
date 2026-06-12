package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IGunModel {
    String DEFAULT_FARTHEST_SIGHT_Z_NAME = "FARTHEST_SIGHT_Z";
    String DEFAULT_HAND_ROT_PIVOT_NAME = "HAND_ROT_PIVOT";

    String getFarthestSightZName(ModuleRenderContext context);

    Bone getHandRotPivot();
}
