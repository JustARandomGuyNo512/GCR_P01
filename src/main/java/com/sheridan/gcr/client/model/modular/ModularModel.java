package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.render.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ModularModel extends BufferedBoneMeshModel implements IModularModel{
    protected ResourceLocation heatMapTexPath = null;
    protected float heatSensitive = 1;

    public ModularModel(MeshModelData root, ResourceLocation name) {
        super(root, name);
    }

    @Override
    protected void readFromMeshData(MeshModelData root) {
        preProcess(root);
        super.readFromMeshData(root);
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        super.updateBoneRenderStatus(context.poseStack, context.light);
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context.isFirstPerson(), context.partialTicks);
    }

    @Override
    protected void afterUniformLoaded(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass, float partialTicks) {
        if (isFirstPerson && !isShadowPass) {
            if (Client.isUsingIrisShader) {
                IrisExtendRT.setUpDrawBuffers();
            }
            uploadMuzzleFlashEffectUniforms(shader.getId());
            uploadHeatMapTex(heatSensitive, shader.getId(), false, heatMapTexPath, partialTicks);
        }
    }

    public static void uploadHeatMapTex(float heatSensitive, int shaderId, boolean forceUseEmptyHeatMap, ResourceLocation heatMapTexPath, float partialTicks) {
        int heatUni = GL20.glGetUniformLocation(shaderId, "gcrHeat");
        int heatMapTexUni = GL20.glGetUniformLocation(shaderId, "gcrHeatMap");
        if (heatUni == -1 || heatMapTexUni == -1) {
            return;
        }
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + (Client.MAX_SHADER_TEXTURES - 1));
        int emptyId = HeatMapTextureManager.getEmptyId();
        int texId1 = HeatMapTextureManager.getTexId(heatMapTexPath);
        int texId = forceUseEmptyHeatMap ? emptyId : texId1;
        float shaderFactor = Client.isUsingIrisShader ? 5 : 4;
        float heat = DrawHolsterHandler.get().getRenderLockedGunHeat(partialTicks);
        heat = (Math.max(0, heat - 0.2f) / 0.8f);
        heat = (float) Math.pow(heat, 1.5f);
        heat *= heatSensitive;
        heat = Math.min(1, heat);
        GL20.glUniform1f(heatUni, heat * shaderFactor);
        RenderSystem.bindTexture(texId);
        GL20.glUniform1i(heatMapTexUni, Client.MAX_SHADER_TEXTURES - 1);
    }

    public static void uploadMuzzleFlashEffectUniforms(int shaderId) {
        Vector3f muzzleFlashPos = Client.WEAPON_STATUS.getMuzzleFlashPos();
        if (Client.isUsingIrisShader) {
            IrisExtendRT.setUpDrawBuffers();
        }
        if (muzzleFlashPos != null) {
            float progress = (Client.getGunRenderer().getCurrFPRenderTimeStampNano() - Client.WEAPON_STATUS.lastShoot) * 1e-9f;
            int muzzleFlashPosition = GL20.glGetUniformLocation(shaderId, "MuzzleFlashPosition");
            int muzzleFlashIntensity = GL20.glGetUniformLocation(shaderId, "MuzzleFlashIntensity");
            int muzzleFlashRadius = GL20.glGetUniformLocation(shaderId, "MuzzleFlashRadius");
            if (muzzleFlashPosition == -1 || muzzleFlashIntensity == -1 || muzzleFlashRadius == -1) {
                return;
            }
            if (progress < 0.1f) {
                progress = progress >= 0.05f ? 0 : (0.05f - progress) * 20f;
                float r = Client.WEAPON_STATUS.getMuzzleFlashRadius();
                if (Client.isUsingIrisShader) {
                    r *= 0.25f;
                }
                GL20.glUniform3f(muzzleFlashPosition, muzzleFlashPos.x, muzzleFlashPos.y, muzzleFlashPos.z);
                GL20.glUniform1f(muzzleFlashIntensity, progress * Client.WEAPON_STATUS.getMuzzleFlashIntensity());
                GL20.glUniform1f(muzzleFlashRadius, r);
            } else {
                GL20.glUniform1f(muzzleFlashIntensity, 0);
                Client.WEAPON_STATUS.clearMuzzleFlashModelEffect();
            }
        }

    }

    @Override
    public @Nullable PoseStack.Pose getBonePose(String name) {
        return getBoneRenderPose(name);
    }

    @Override
    public @NotNull PoseStack.Pose getRootPose() {
        return rootBone.renderStatus.pose;
    }

    @Override
    public @NotNull Bone getRootBone() {
        return rootBone;
    }

    @Override
    public boolean hasSlot(String name) {
        return hasBone(name);
    }

    protected void preProcess(MeshModelData root) {}

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        this.resetPose();
    }

    @Override
    public void preFirstPersonRender(FirstPersonRenderContext context) {

    }

    @Override
    public void offsetPos(Vector3f vector3f) {
        this.rootBone.offsetPos(vector3f);
    }

    @Override
    public void offsetRotation(Vector3f vector3f) {
        this.rootBone.offsetRotation(vector3f);
    }

    @Override
    public void offsetScale(Vector3f vector3f) {
        this.rootBone.offsetScale(vector3f);
    }

    @Override
    public Optional<IAnimated> findByName(String pName) {
        return this.rootBone.findByName(pName);
    }
    public void hide(String name) {
        Bone bone = getBone(name);
        if (bone != null) {
            bone.xScale = 0;
            bone.yScale = 0;
            bone.zScale = 0;
            bone.renderStatus.visible = false;
        }
    }

    @Override
    public void copyRenderStatus(Map<String, BoneRenderStatus> statusStorage) {
        renderingVertexCount = 0;
        for (Map.Entry<String, Bone> entry : flatBoneMap.entrySet()) {
            String key = entry.getKey();
            BoneRenderStatus status = statusStorage.get(key);
            BoneRenderStatus renderStatus = entry.getValue().renderStatus;
            if (status == null || status == renderStatus) {
                continue;
            }
            renderStatus.copyFrom(status);
            if (status.visible) {
                renderingVertexCount += renderStatus.vertexCount;
            }
        }
    }

    @Override
    public IModularModel setHeatMapTexPath(ResourceLocation path) {
        this.heatMapTexPath = path;
        return this;
    }

    @Override
    public @Nullable ResourceLocation getHeatMapTexPath() {
        return heatMapTexPath;
    }


    public Bone getOrThrow(String boneName) {
        Bone bone = getBone(boneName);
        if (bone == null) {
            throw new RuntimeException("can't find bone: " + boneName);
        }
        return bone;
    }

    public ModularModel setDebugName(ResourceLocation name) {
        this.debugName = name;
        return this;
    }

    @Override
    public IModularModel modifyHeatSensitive(float heatSensitive) {
        heatSensitive = Math.max(heatSensitive, 0.1f);
        this.heatSensitive = heatSensitive;
        return this;
    }
}
