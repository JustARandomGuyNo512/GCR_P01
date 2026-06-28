package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.render.AuxRenderTarget;
import com.sheridan.gcr.client.render.FirstPersonRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import com.sheridan.gcr.client.render.fx.DepthCopyShader;
import com.sheridan.gcr.client.render.fx.ScopeViewShadingShader;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ScopeModel extends AbstractScopeModel{
    public static final int DEFERRED_TASK_PUSHED = 9800;
    public static final int SCOPE_VIEW_RENDERING = 9799;
    private static final Vector3f DIRECTION = new Vector3f();
    private static final int CROSSHAIR_POSE = 9796;
    private static final int REAR_LENS_POSE = 9797;
    private static final int SCOPE_NODE_ID = 9798;

    private final float viewRadius;
    private final float shadingSensitivity;
    private final float shadingInnerFade;
    private final float shadingOuterFade;
    private final float vignettePower;

    public ScopeModel(MeshModelData root, ResourceLocation name,
                      float viewRadius, float shadingSensitivity, float shadingInnerFade,
                      float shadingOuterFade, float vignettePower, float crosshairScale,
                      ResourceLocation crosshairTexture) {
        super(root, name, crosshairTexture, crosshairScale);
        this.viewRadius = viewRadius;
        this.shadingSensitivity = shadingSensitivity;
        this.shadingInnerFade = shadingInnerFade;
        this.shadingOuterFade = shadingOuterFade;
        this.vignettePower = vignettePower;
    }

    /**
     * 没有使用iris的情况下，直接使用深度检测做镜片剔除
     * */
    @Override
    public void preFirstPersonRender(FirstPersonRenderContext context) {
        super.preFirstPersonRender(context);
        if (shouldHandlerScopeRender(context)) {
            if (Client.isIrisShaderInUse) {
                AuxRenderTarget.getInstance().check();
                AuxRenderTarget.getInstance().bindWrite(true);
                RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, AuxRenderTarget.getInstance().frameBufferId);
                GL30.glBlitFramebuffer(
                        0, 0, main.width, main.height,
                        0, 0, main.width, main.height,
                        GL30.GL_DEPTH_BUFFER_BIT,
                        GL30.GL_NEAREST
                );
                main.bindWrite(true);
                context.setLocalStorage(MuzzleFlashRenderer.RENDER_CANCELED, 1);
            }

            if (!Client.isIrisShaderInUse) {
                renderRearLensWithStencil(0);
                GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                renderCrosshairTexture(
                        getCrosshairBone().renderStatus.pose,
                        getCrosshairZBone().renderStatus.pose
                );
                clearAndDisableStencil();
            }

            Bone rearLensBone = getRearLensBone();
            PoseStack.Pose copy = rearLensBone.renderStatus.pose.copy();
            renderRearLensOnly = true;
            rearLensBone.renderStatus.visible = true;
            float zOffset = Client.isIrisShaderInUse ? -0.0085f : -0.002f;
            rearLensBone.renderStatus.pose.pose().translate(0, 0, zOffset);
            RenderType original = getRenderType();
            setRenderType(RenderTypes.getMeshDepthMask(), false);
            super.render(true);
            setRenderType(original, false);
            renderRearLensOnly = false;

            if (Client.isIrisShaderInUse) {
                context.setLocalStorage(CROSSHAIR_POSE, getCrosshairBone().renderStatus.pose.copy());
            }
            context.setLocalStorage(SCOPE_NODE_ID, context.currentRenderNode().id);
            context.setLocalStorage(SCOPE_VIEW_RENDERING, 1);
            context.setLocalStorage(REAR_LENS_POSE, copy);
        }
    }

    @Override
    public void render(ModuleRenderContext context) {
        if (shouldHandlerScopeRender(context)) {
            getRearLensBone().renderStatus.visible = false;
        }
        super.render(context);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        if (context.getLocalStorage(SCOPE_VIEW_RENDERING) != null &&
                !Objects.equals(context.currentRenderNode().id, context.getLocalStorage(SCOPE_NODE_ID))) {
            return;
        }
        PoseStack.Pose crosshairPose = context.getLocalStorage(CROSSHAIR_POSE, PoseStack.Pose.class);
        PoseStack.Pose rearLensPose = context.getLocalStorage(REAR_LENS_POSE, PoseStack.Pose.class);
        PoseStack.Pose crosshairZPose = getCrosshairZBone().renderStatus.pose;
        context.removeLocalStorage(REAR_LENS_POSE);
        context.removeLocalStorage(SCOPE_NODE_ID);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        try {
            if (Client.isIrisShaderInUse && !IrisCompat.isRenderingShadowPass()) {
                if (crosshairPose == null || rearLensPose == null || crosshairZPose == null) {
                    GL11.glDisable(GL11.GL_STENCIL_TEST);
                    return;
                }
                context.removeLocalStorage(CROSSHAIR_POSE);
                Bone rearLensBone = getRearLensBone();
                rearLensBone.renderStatus.pose = rearLensPose.copy();
                renderRearLensWithStencil(-0.007f);

                GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

                //还原深度缓冲
                GL11.glDepthFunc(GL11.GL_ALWAYS);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true);
                GL20.glUseProgram(DepthCopyShader.programId);
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(AuxRenderTarget.getInstance().getDepthTextureId());
                Uniform.uploadInteger(DepthCopyShader.depthSamplerLoc, 0);
                GL30.glBindVertexArray(DepthCopyShader.vaoId);
                GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
                GL30.glBindVertexArray(0);
                ProgramManager.glUseProgram(0);
                GL11.glDepthFunc(GL11.GL_LEQUAL);

                renderCrosshairTexture(crosshairPose, crosshairZPose);
                //防止光影漏光
                rearLensBone.renderStatus.pose = rearLensPose.copy();
                renderRearLensWithStencil(0.0005f);

                copyStencil();
                clearAndDisableStencil();

                //延迟任务只记录一次!
                if (context.getLocalStorage(DEFERRED_TASK_PUSHED) == null) {
                    final Matrix4f modelViewMat = RenderSystem.getModelViewMatrix();
                    Stage.LOW.addTask(
                            new Task((event) -> shaderDeferredRender(modelViewMat, rearLensPose)));
                    context.setLocalStorage(DEFERRED_TASK_PUSHED, 1);
                }

            } else {
                if (rearLensPose == null) {
                    GL11.glDisable(GL11.GL_STENCIL_TEST);
                    return;
                }
                getRearLensBone().renderStatus.pose = rearLensPose.copy();
                renderRearLensWithStencil(0);
                GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                renderCrosshairShading(rearLensPose);
                clearAndDisableStencil();
            }
        } finally {
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    }

    private void copyStencil() {
        AuxRenderTarget.getInstance().check();
        AuxRenderTarget.getInstance().bindWrite(true);
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, AuxRenderTarget.getInstance().frameBufferId);
        GL30.glBlitFramebuffer(
                0, 0, main.width, main.height,
                0, 0, main.width, main.height,
                GL30.GL_STENCIL_BUFFER_BIT,
                GL30.GL_NEAREST
        );
        main.bindWrite(true);
    }

    private void blitStencil() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, AuxRenderTarget.getInstance().frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GL30.glBlitFramebuffer(
                0, 0, main.width, main.height,
                0, 0, main.width, main.height,
                GL30.GL_STENCIL_BUFFER_BIT,
                GL30.GL_NEAREST
        );
        main.bindWrite(false);
    }

    private void renderRearLensWithStencil(float zOffset) {
        Bone rearLensBone = getRearLensBone();
        rearLensBone.renderStatus.visible = true;
        rearLensBone.renderStatus.pose.pose().translate(0, 0, zOffset);
        GL11.glStencilMask(0xFF);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        renderRearLensOnly = true;
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderType original = getRenderType();
        setRenderType(RenderTypes.getMeshStencilMask(), false);
        renderingVertexCount = 1;
        super.render(true);
        setRenderType(original, false);
        renderRearLensOnly = false;
        GL11.glStencilMask(0x00);
    }

    public void shaderDeferredRender(Matrix4f modelViewMat, PoseStack.Pose rearLensPose) {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(Client.FIRST_PERSON_PROJECTION_MAT, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewMatrix().set(modelViewMat);

        blitStencil();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        MuzzleFlashRenderer.renderAllFirstPerson(bufferSource);
        bufferSource.endBatch();

        renderCrosshairShading(rearLensPose);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.restoreProjectionMatrix();
        clearAndDisableStencil();
    }

    private void renderCrosshairShading(PoseStack.Pose rearLens) {
        GL11.glStencilMask(0x00);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.colorMask(true, true, true, true);
        Window window = Minecraft.getInstance().getWindow();
        float width = window.getWidth();
        float height = window.getHeight();
        Matrix4f mat = rearLens.pose();
        DIRECTION.set(0, 0, -1);
        mat.transformDirection(DIRECTION);
        DIRECTION.normalize();
        Vector3f lensPos = mat.getTranslation(new Vector3f());
        Vector3f lensForward = new Vector3f(DIRECTION).normalize();
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f lensRight = new Vector3f(lensForward).cross(worldUp).normalize();
        Vector3f lensUp = new Vector3f(lensRight).cross(lensForward).normalize();
        Vector3f eyeToLens = new Vector3f(lensPos).normalize();
        float offsetX = eyeToLens.dot(lensRight);
        float offsetY = eyeToLens.dot(lensUp);
        float distance = lensPos.length();
        Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrix();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
        Vector3f screenPos = Utils.getScreenPos(mat, modelViewMatrix, projectionMatrix, width, height);
        Matrix4f edgeMat = mat.translate(lensRight.mul(viewRadius));
        Vector3f edgePos = Utils.getScreenPos(edgeMat, modelViewMatrix, projectionMatrix, width, height);
        float viewEdge = edgePos.sub(screenPos).length();
        GL20.glUseProgram(ScopeViewShadingShader.programId);
        GL20.glUniform2f(ScopeViewShadingShader.uEyeOffsetLoc, offsetX, offsetY);
        GL20.glUniform1f(ScopeViewShadingShader.uEyeDistanceLoc, distance);
        GL20.glUniform2f(ScopeViewShadingShader.uResolutionLoc, width, height);
        GL20.glUniform1f(ScopeViewShadingShader.uLensRadiusLoc, viewEdge);
        GL20.glUniform2f(ScopeViewShadingShader.uLensCenterLoc, screenPos.x, screenPos.y);
        GL20.glUniform1f(ScopeViewShadingShader.uSensitivityLoc, shadingSensitivity);

        GL20.glUniform1f(ScopeViewShadingShader.uInnerFadeLoc, shadingInnerFade);
        GL20.glUniform1f(ScopeViewShadingShader.uOuterFadeLoc, shadingOuterFade);
        GL20.glUniform1f(ScopeViewShadingShader.uVignettePowerLoc, vignettePower);

        GL30.glBindVertexArray(ScopeViewShadingShader.vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
        ProgramManager.glUseProgram(0);

        RenderSystem.disableBlend();
    }
}
