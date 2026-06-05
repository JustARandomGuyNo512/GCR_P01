package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.IrisExtendRT;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;

import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ModularModel extends BufferedBoneMeshModel implements IModularModel{

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
        super.render(context.isFirstPerson());
    }

    @Override
    protected void afterUniformLoaded(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass) {
        if (isFirstPerson && !isShadowPass) {
            uploadMuzzleFlashEffectUniforms(shader.getId());
        }
    }

    public static void uploadMuzzleFlashEffectUniforms(int shaderId) {
        Vector3f muzzleFlashPos = Client.WEAPON_STATUS.getMuzzleFlashPos();
        if (muzzleFlashPos != null) {
            float progress = Client.distFromLastShoot();
            int muzzleFlashPosition = GL20.glGetUniformLocation(shaderId, "MuzzleFlashPosition");
            int muzzleFlashIntensity = GL20.glGetUniformLocation(shaderId, "MuzzleFlashIntensity");
            int muzzleFlashRadius = GL20.glGetUniformLocation(shaderId, "MuzzleFlashRadius");
            if (muzzleFlashPosition == -1 || muzzleFlashIntensity == -1 || muzzleFlashRadius == -1) {
                return;
            }
            if (progress < 0.1f) {
                progress = progress >= 0.05f ? 0 : (0.05f - progress) * 20f;
                float r = Client.WEAPON_STATUS.getMuzzleFlashRadius();

                GL20.glUniform3f(muzzleFlashPosition, muzzleFlashPos.x, muzzleFlashPos.y, muzzleFlashPos.z);
                GL20.glUniform1f(muzzleFlashIntensity, progress * Client.WEAPON_STATUS.getMuzzleFlashIntensity());
                GL20.glUniform1f(muzzleFlashRadius, r);
                if (Client.isIrisShaderInUse) {
                    IrisExtendRT.setUpDrawBuffers();
                }
            } else {
                GL20.glUniform1f(muzzleFlashIntensity, 0);
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
}
