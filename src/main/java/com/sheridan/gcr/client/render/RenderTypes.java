package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sheridan.gcr.GCR;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.BiFunction;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class RenderTypes extends RenderType  {
    public static final ResourceLocation WHITE = GCR.RL("textures/white.png");

    static Function<ResourceLocation, RenderType> MESH_CUTOUT = Util.memoize((location) -> {
        CompositeState compositeState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new TextureStateShard(location, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create("mesh_cut_out", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, false, compositeState);
    });

    static Function<ResourceLocation, RenderType> MESH_CUTOUT_NO_CULL = Util.memoize((location) -> {
        CompositeState compositeState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new TextureStateShard(location, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setCullState(NO_CULL)
                .createCompositeState(true);
        return create("mesh_cut_out_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, false, compositeState);
    });

    static Function<ResourceLocation, RenderType> MESH_STENCIL_MASK = Util.memoize((location) ->
            RenderType.create("stencil_mask", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                    .setTextureState(new TextureStateShard(location, false, false))
                    .setWriteMaskState(new WriteMaskStateShard(false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    ));

    static Function<ResourceLocation, RenderType> MESH_DEPTH_MASK = Util.memoize((location) ->
            RenderType.create("depth_mask", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                            .setTextureState(new TextureStateShard(location, false, false))
                            .setWriteMaskState(new WriteMaskStateShard(false, true))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .createCompositeState(false)
            ));

    public static final BiFunction<ResourceLocation, Boolean, RenderType> SLOW_MUZZLE_SMOKE = Util.memoize(
            (p_286156_, p_286157_) -> {
                RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(p_286156_, false, false))
                        .setWriteMaskState(new WriteMaskStateShard(true, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(p_286157_);
                return create("slow_muzzle_smoke", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
            }
    );

    public static RenderType getSlowMuzzleSmoke(ResourceLocation location) {
        return SLOW_MUZZLE_SMOKE.apply(location, true);
    }

    public RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getMeshCutOut(ResourceLocation location) {
        return MESH_CUTOUT.apply(location);
    }

    public static RenderType getMeshCutOutNoCull(ResourceLocation location) {
        return MESH_CUTOUT_NO_CULL.apply(location);
    }

    public static RenderType getMeshStencilMask() {
        return MESH_STENCIL_MASK.apply(WHITE);
    }

    public static RenderType getMeshDepthMask() {
        return MESH_DEPTH_MASK.apply(WHITE);
    }

    public static RenderType getMuzzleFlash(ResourceLocation texture) {
        return RenderType.create("muzzle_flash", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, true, true,
                CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setLightmapState(LightmapStateShard.LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .createCompositeState(false));
    }

}
