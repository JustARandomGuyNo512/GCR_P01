package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.modules.SightModel;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractScopeModel extends SightModel implements IScopeModel {
    private final ResourceLocation crosshairTexture;
    protected boolean renderRearLensOnly = false;
    private final float crosshairScale;

    public AbstractScopeModel(MeshModelData root, ResourceLocation name, ResourceLocation crosshairTexture, float crosshairScale) {
        super(root, name);
        this.crosshairTexture = crosshairTexture;
        this.crosshairScale = crosshairScale;
    }

    protected boolean shouldHandlerScopeRender(ModuleRenderContext context) {
        boolean b = Client.getAimingProgress() > 0.9f &&
                !IrisCompat.isRenderingShadowPass() &&
                Utils.isStencilEnabled() &&
                Client.WEAPON_STATUS.isSightActivated(context.currentRenderNode().id);
        if (Client.isUsingIrisShader) {
            return BufferedBoneMeshModel.isCurrentSupportGcrRender() && b;
        }
        return b;
    }

    @Override
    public @NotNull Bone getRearLensBone() {
        return getBone("REAR_LENS");
    }

    @Override
    public @NotNull Bone getCrosshairBone() {
        return getBone("CROSS_HAIR");
    }

    @Override
    public ResourceLocation getCrosshairTexture() {
        return crosshairTexture;
    }

    @Override
    public void draw(int vertexCount, int indices) {
        if (renderRearLensOnly) {
            Bone rearGlassBone = getRearLensBone();
            super.draw(rearGlassBone.vertexCount, rearGlassBone.renderStatus.vertexStart);
        } else {
            super.draw(vertexCount, indices);
        }
    }

    protected void clearAndDisableStencil() {
        GL11.glStencilMask(0xFF);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0x00);
    }

    protected void renderCrosshairTexture(PoseStack.Pose crosshair, PoseStack.Pose crosshairZ) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, getCrosshairTexture());

        Matrix4f farMat = crosshair.pose();
        Matrix4f nearMat = crosshairZ.pose();

        // 提取位置
        Vector3f farPos = farMat.getTranslation(new Vector3f());
        Vector3f nearPos = nearMat.getTranslation(new Vector3f());

        // 透视投影保持一致：
        // x/z 与 y/z 保持不变
        float scale = nearPos.z / farPos.z;

        Vector3f projectedPos = new Vector3f(
                farPos.x * scale,
                farPos.y * scale,
                nearPos.z
        );
        float crosshairScale = getCrosshairScale();
        if (Client.isUsingIrisShader) {
            crosshairScale *= 0.5f;
        }
        // 构造新的 matrix
        Matrix4f mat = new Matrix4f()
                .translation(projectedPos)
                .rotate(nearMat.getNormalizedRotation(new Quaternionf()))
                .scale(crosshairScale);

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        buffer.addVertex(mat, -0.5f, -0.5f, 0).setUv(0, 1);
        buffer.addVertex(mat,  0.5f, -0.5f, 0).setUv(1, 1);
        buffer.addVertex(mat,  0.5f,  0.5f, 0).setUv(1, 0);
        buffer.addVertex(mat, -0.5f,  0.5f, 0).setUv(0, 0);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

//    protected void renderCrosshairTexture(PoseStack.Pose crosshair, PoseStack.Pose crosshairZ) {
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        RenderSystem.setShaderTexture(0, getCrosshairTexture());
//
//        Matrix4f mat = new Matrix4f(crosshair.pose());
//        mat.scale(getCrosshairScale());
//        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//
//        buffer.addVertex(mat, -0.5f, -0.5f, 0).setUv(0, 1);
//        buffer.addVertex(mat,  0.5f, -0.5f, 0).setUv(1, 1);
//        buffer.addVertex(mat,  0.5f,  0.5f, 0).setUv(1, 0);
//        buffer.addVertex(mat, -0.5f,  0.5f, 0).setUv(0, 0);
//
//        BufferUploader.drawWithShader(buffer.buildOrThrow());
//    }
    @Override
    public @NotNull Bone getCrosshairZBone() {
        return getBone("CROSS_HAIR_Z");
    }

    public float getCrosshairScale() {
        return crosshairScale;
    }
}
