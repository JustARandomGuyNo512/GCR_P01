package com.sheridan.gcr.client.render;

import com.sheridan.gcr.client.model.BoneRenderStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public interface IRenderStatusCacheContext {
    Map<ModuleRenderNode, Map<String, BoneRenderStatus>> getRenderStatusMap();
}
