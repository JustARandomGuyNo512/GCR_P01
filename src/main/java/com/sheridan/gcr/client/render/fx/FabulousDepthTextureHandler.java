package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
public class FabulousDepthTextureHandler {
    public static TextureTarget fabulousDepthMergeTarget;
    public static boolean updateFrame = false;
    @SubscribeEvent
    public static void update(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER &&
                Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            //merge depth for fabulous mode
            if (updateFrame && Minecraft.useShaderTransparency()) {
                Minecraft minecraft = Minecraft.getInstance();
                LevelRenderer levelRenderer = event.getLevelRenderer();
                RenderTarget main = minecraft.getMainRenderTarget();

                RenderTarget translucentTarget = levelRenderer.getTranslucentTarget();
                RenderTarget itemEntityTarget = levelRenderer.getItemEntityTarget();
                RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
                RenderTarget cloudsTarget = levelRenderer.getCloudsTarget();
                RenderTarget weatherTarget = levelRenderer.getWeatherTarget();
                if (translucentTarget != null && itemEntityTarget != null && particlesTarget != null && cloudsTarget != null && weatherTarget != null) {
                    Window window = minecraft.getWindow();
                    int width = window.getWidth();
                    int height = window.getHeight();
                    if (fabulousDepthMergeTarget == null) {
                        fabulousDepthMergeTarget = new TextureTarget(width, height, false, true);
                    } else if (fabulousDepthMergeTarget.width != width || fabulousDepthMergeTarget.height != height) {
                        fabulousDepthMergeTarget.resize(width, height, true);
                    }

                    int texMainDepth = main.getDepthTextureId();
                    int texTranslucentDepth = translucentTarget.getDepthTextureId();
                    int texItemDepth = itemEntityTarget.getDepthTextureId();
                    int texParticleDepth = particlesTarget.getDepthTextureId();
                    int texCloudDepth = cloudsTarget.getDepthTextureId();
                    int texWeatherDepth = weatherTarget.getDepthTextureId();

                    fabulousDepthMergeTarget.bindWrite(false);
                    GL20.glUseProgram(FabulousMergeDepthShader.programId);

                    RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                    RenderSystem.bindTexture(texMainDepth);
                    Uniform.uploadInteger(FabulousMergeDepthShader.mainDepthLoc, 0);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE1);
                    RenderSystem.bindTexture(texTranslucentDepth);
                    Uniform.uploadInteger(FabulousMergeDepthShader.translucentDepthLoc, 1);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE2);
                    RenderSystem.bindTexture(texItemDepth);
                    Uniform.uploadInteger(FabulousMergeDepthShader.itemDepthLoc, 2);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE3);
                    RenderSystem.bindTexture(texParticleDepth);
                    Uniform.uploadInteger(FabulousMergeDepthShader.particlesDepthLoc, 3);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE4);
                    RenderSystem.bindTexture(texCloudDepth);
                    Uniform.uploadInteger(FabulousMergeDepthShader.cloudsDepthLoc, 4);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE5);
                    RenderSystem.bindTexture(texWeatherDepth);
                    Uniform.uploadInteger(FabulousMergeDepthShader.weatherDepthLoc, 5);

                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    GL30.glBindVertexArray(FabulousMergeDepthShader.vaoId);
                    GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
                    GL30.glBindVertexArray(0);
                    ProgramManager.glUseProgram(0);

                    RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                    RenderSystem.bindTexture(0);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE1);
                    RenderSystem.bindTexture(0);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE2);
                    RenderSystem.bindTexture(0);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE3);
                    RenderSystem.bindTexture(0);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE4);
                    RenderSystem.bindTexture(0);
                    RenderSystem.activeTexture(GL13.GL_TEXTURE5);
                    RenderSystem.bindTexture(0);
                    RenderSystem.enableDepthTest();
                }
            }
            updateFrame = false;
        }
    }

    public static void mergeDepthThisFrame() {
        updateFrame = true;
    }

    public static int getDepthTextureId() {
        if (Minecraft.useShaderTransparency() && fabulousDepthMergeTarget != null) {
            return fabulousDepthMergeTarget.getColorTextureId();
        }
        return Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
    }
}
