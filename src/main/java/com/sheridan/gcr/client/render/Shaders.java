package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.sheridan.gcr.GCR;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

public class Shaders {
    private static final java.util.Set<String> DIAG_SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Logs a render-path diagnostic message once per key (avoids per-frame spam). */
    public static void diagOnce(String key, String message) {
        if (DIAG_SEEN.add(key)) {
            GCR.LOGGER.warn("[GCR RenderDiag] {}", message);
        }
    }


    public static ShaderInstance entityCutOutUBO;

    public static ShaderInstance getEntityCutOutUBO() {
        return entityCutOutUBO;
    }

    public static void init(ResourceProvider provider) {
        try {
            entityCutOutUBO = new ShaderInstance(provider, GCR.RL(GCR.MODID, "entity_cutout_ubo"), DefaultVertexFormat.NEW_ENTITY);
            GCR.LOGGER.info("[GCR] entity_cutout_ubo shader loaded, program id {}", entityCutOutUBO.getId());
        } catch (Exception e) {
            GCR.LOGGER.error("[GCR] failed to load entity_cutout_ubo shader", e);
        }
    }
}
