package com.sheridan.gcr.client.model.playerArm;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.gltf.io.GltfModelLoader;
import com.sheridan.gcr.client.model.modular.ModularModel;
import com.sheridan.gcr.client.render.RenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BufferedPlayerArmModel extends BufferedBoneMeshModel {
    public static final ResourceLocation PATH = GCR.RL(GCR.MODID, "model_assets/gltf/arms.gltf");
    private static BufferedPlayerArmModel instance = null;

    private final Bone L_MALE;
    private final Bone L_SLIM;
    private final Bone R_MALE;
    private final Bone R_SLIM;

    public static BufferedPlayerArmModel getInstance() {
        if (instance == null) {
            init();
        }
        return instance;
    }

    public BufferedPlayerArmModel(MeshModelData root, ResourceLocation name) {
        super(root, name);
        L_MALE = flatBoneMap.get("L_MALE");
        L_SLIM = flatBoneMap.get("L_SLIM");
        R_MALE = flatBoneMap.get("R_MALE");
        R_SLIM = flatBoneMap.get("R_SLIM");
        if (L_MALE == null) {
            throw new RuntimeException("Can't find bone L_MALE");
        }
        if (L_SLIM == null) {
            throw new RuntimeException("Can't find bone L_SLIM");
        }
        if (R_MALE == null) {
            throw new RuntimeException("Can't find bone R_MALE");
        }
        if (R_SLIM == null) {
            throw new RuntimeException("Can't find bone R_SLIM");
        }
    }

    public void record(PoseStack.Pose pose, boolean leftArm, boolean slim, int light) {
        Bone bone;
        if (leftArm) {
            bone = slim ? L_SLIM : L_MALE;
        } else {
            bone = slim ? R_SLIM : R_MALE;
        }
        bone.renderStatus.visible = true;
        bone.renderStatus.pose = pose;
        bone.renderStatus.lightmapUV = light;
        renderingVertexCount = 1;
    }

    @Override
    protected void afterUniformLoaded(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass, float partialTicks) {
        super.afterUniformLoaded(shader, isFirstPerson, isShadowPass, 0);
        if (isFirstPerson && !isShadowPass) {
            //ModularModel.uploadHeatMapTex(shader.getId(), true, null);
            ModularModel.uploadMuzzleFlashEffectUniforms(shader.getId());
        }
    }

    public void hideAll() {
        L_MALE.renderStatus.visible = false;
        L_SLIM.renderStatus.visible = false;
        R_MALE.renderStatus.visible = false;
        R_SLIM.renderStatus.visible = false;
        renderingVertexCount = 0;
    }

    @Override
    public void render(boolean isFirstPerson, float partialTicks) {
        super.render(isFirstPerson, 0);
    }

    public boolean checkSkinTexture() {
        ResourceLocation texture = getPlayerSkin();
        if (texture != null) {
            RenderType meshCutOut = RenderTypes.getMeshCutOutNoCull(texture);
            setRenderType(meshCutOut, false);
            return true;
        }
        return false;
    }

    public static boolean isPlayerModelSlim() {
        AbstractClientPlayer abstractClientPlayer = Minecraft.getInstance().player;
        if (abstractClientPlayer != null) {
            return abstractClientPlayer.getSkin().model() == PlayerSkin.Model.SLIM;
        }
        return false;
    }



    public static ResourceLocation getPlayerSkin() {
        AbstractClientPlayer abstractClientPlayer = Minecraft.getInstance().player;
        if (abstractClientPlayer != null) {
            return abstractClientPlayer.getSkin().texture();
        }
        return null;
    }

    public static void init() {
        if (instance != null) {
            return;
        }
        MeshModelData data = GltfModelLoader.loadModel(PATH);
        instance = new BufferedPlayerArmModel(data, GCR.RL("player_arm"));
        RenderSystem.recordRenderCall(() -> instance.compile(RenderTypes.getMeshCutOutNoCull(RenderTypes.WHITE)));
    }
}
