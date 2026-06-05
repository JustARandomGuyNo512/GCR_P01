package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.sheridan.gcr.GCR;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

public class Shaders {

    public static ShaderInstance entityCutOutUBO;

    public static ShaderInstance getEntityCutOutUBO() {
        return entityCutOutUBO;
    }

    public static void init(ResourceProvider provider) {
        try {
            entityCutOutUBO = new ShaderInstance(provider, GCR.RL(GCR.MODID, "entity_cutout_ubo"), DefaultVertexFormat.NEW_ENTITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
