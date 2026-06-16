package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.*;

import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;

@OnlyIn(Dist.CLIENT)
public class FPMuzzleFlashEnvLightingRenderer {
    public static boolean shouldDraw = false;
    public static float progress = 0;

    static {
        Stage.HIGH.addTask(new Task((event) -> onRender()).forever());
    }

    @SubscribeEvent
    public static void handleProgress(RenderFrameEvent.Pre event) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            shouldDraw = Client.WEAPON_STATUS.isHoldingGun();
            if (shouldDraw) {
                float dist = Client.distFromLastShoot();
                if (dist <= 0.05f) {
                    progress = 0.05f - dist;
                } else {
                    shouldDraw = false;
                }
            } else {
                progress = 0;
            }
            if (shouldDraw) {
                FabulousDepthTextureHandler.mergeDepthThisFrame();
            }
        }
    }

    public static void onRender() {
        if (shouldDraw) {
            Minecraft instance = Minecraft.getInstance();
            if (!instance.options.getCameraType().isFirstPerson()) {
                return;
            }
            handleEffect(progress * 3f);
            GL42.glMemoryBarrier(GL42.GL_TEXTURE_FETCH_BARRIER_BIT);
        }

    }

    public static void handleEffect(float intensity) {
        if (intensity <= 0 || !MuzzleFlashEnvShader.isOK()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        GL20.glUseProgram(MuzzleFlashEnvShader.programId);

        int depthTexId = FabulousDepthTextureHandler.getDepthTextureId();
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(depthTexId);
        float cameraFar = mc.gameRenderer.getDepthFar();
        Uniform.uploadInteger(MuzzleFlashEnvShader.gdepthLoc, 0);
        GL20.glUniform1f(MuzzleFlashEnvShader.flashIntensityLoc, intensity);
        GL20.glUniform1f(MuzzleFlashEnvShader.minDepthLoc, Client.isIrisShaderInUse ? 0.15f : 0f);
        GL20.glUniform1f(MuzzleFlashEnvShader.maxDepthLoc, 12f);
        GL20.glUniform1f(MuzzleFlashEnvShader.lightRadiusLoc, 9.5f);
        GL20.glUniform1f(MuzzleFlashEnvShader.cameraNFLoc, 0.1f * cameraFar);
        GL20.glUniform1f(MuzzleFlashEnvShader.cameraN_FLoc, 0.05f + cameraFar);
        GL20.glUniform1f(MuzzleFlashEnvShader.cameraNFDistLoc, cameraFar - 0.05f);
        int width = window.getWidth();
        int height = window.getHeight();
        GL20.glUniform1f(MuzzleFlashEnvShader.aspectRatioLoc, (float) width / (float) height);
        GL20.glUniform2f(MuzzleFlashEnvShader.texelSizeLoc, (float) 1 / width, (float) 1 / height);
        GL20.glUniform1i(MuzzleFlashEnvShader.isFabulousModeLoc, Minecraft.useShaderTransparency() ? 1 : 0);

        GL30.glBindVertexArray(MuzzleFlashEnvShader.vaoId);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        ProgramManager.glUseProgram(0);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(0);
    }

}
