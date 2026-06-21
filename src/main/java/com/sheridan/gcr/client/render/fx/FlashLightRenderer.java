package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import com.sheridan.gcr.client.render.fx.post.PostChain;
import com.sheridan.gcr.client.render.fx.post.PostPass;
import com.sheridan.gcr.items.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FlashLightRenderer {
    public static PostChain flashlight;
    private static int lastWidth;
    private static int lastHeight;
    private static boolean failedLoadingFlashLightShader = false;
    public static boolean doEffect = false;
    public static float luminance = 0;
    public static float range = 0;
    public static float angle = 0;
    public static Vector3f direction = new Vector3f(0,0,1);

    public static class FlashLightClamp {

        public static final float MAX_LUMINANCE = 30.0f;
        public static final float MAX_RANGE = 150f;
        public static final float MAX_ANGLE = 30f;

        // 控制曲线斜率
        public static final float LUMINANCE_K = -0.15f;
        public static final float RANGE_K = -0.025f;
        public static final float ANGLE_K = -0.1f;

        public static float clampLuminance(float x) {
           double factor = 1 - Math.exp(LUMINANCE_K * x);
           return (float) (Mth.clamp(x, 0, MAX_LUMINANCE) * factor);
        }

        public static float clampRange(float x) {
            double factor = 1 - Math.exp(RANGE_K * x);
            return (float) (Mth.clamp(x, 0, MAX_RANGE) * factor);
        }

        public static float clampAngle(float x) {
            double factor = 1 - Math.exp(ANGLE_K * x);
            return (float) (Mth.clamp(x, 0, MAX_ANGLE) * factor);
        }

    }

    public static void recordEffectCall(float inputLuminance, float inputRange, float inputAngle, Vector3f direction) {
        doEffect = true;
        luminance += inputLuminance;
        angle += inputAngle;
        range += inputRange;
        FlashLightRenderer.direction.set(direction);
    }

    static {
        Stage.HIGHEST.addTask(new Task(FlashLightRenderer::renderFlashLight).forever());
    }

    @SubscribeEvent
    public static void checkFabulousMergeDepth(RenderLevelStageEvent event) {
        if (doEffect && event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && !failedLoadingFlashLightShader) {
            FabulousDepthTextureHandler.mergeDepthThisFrame();
        }
    }

    public static void renderFlashLight(RenderLevelStageEvent event) {
        if (doEffect && !failedLoadingFlashLightShader) {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && stack.getItem() instanceof GunItem) {
                if (flashlight == null) {
                    try {
                        PostChain postChain = new PostChain(Minecraft.getInstance().getTextureManager(),
                                Minecraft.getInstance().getResourceManager(),
                                Minecraft.getInstance().getMainRenderTarget(),
                                GCR.RL("shaders/post/flashlight.json"));
                        lastWidth = Minecraft.getInstance().getWindow().getWidth();
                        lastHeight = Minecraft.getInstance().getWindow().getHeight();
                        postChain.resize(lastWidth, lastHeight);
                        flashlight = postChain;
                        System.out.println("Loaded flashlight shader");
                    } catch (Exception e) {
                        e.printStackTrace();
                        GCR.LOGGER.info(e.getMessage());
                        failedLoadingFlashLightShader = true;
                        player.sendSystemMessage(Component.literal("Error loading flashlight shader!!!").setStyle(Style.EMPTY.withColor(0xFF0000)));
                    }
                } else {
                    if (luminance == 0 || range == 0 || angle == 0) {
                        doEffect = false;
                        return;
                    }
                    int width = Minecraft.getInstance().getWindow().getWidth();
                    int height = Minecraft.getInstance().getWindow().getHeight();
                    if (width != lastWidth || height != lastHeight) {
                        lastHeight = height;
                        lastWidth = width;
                        flashlight.resize(width, height);
                    }

                    Vector3f dir = new Vector3f(FlashLightRenderer.direction);
                    float fovScene = (float) RenderEvents.currentFov;
                    float scale = (float) (
                            Math.tan(Math.toRadians(fovScene * 0.5)) /
                                    Math.tan(Math.toRadians(35))
                    );
                    dir.x *= scale;
                    dir.y *= scale;
                    dir.normalize();
                    FlashLightRenderer.direction.set(dir);

                    List<PostPass> passes = flashlight.passes;
                    Matrix4f inversePerspectiveProjMat = new Matrix4f(RenderSystem.getProjectionMatrix().invert());
                    Matrix4f inverseModelViewMat = new Matrix4f(RenderSystem.getModelViewMatrix().invert());
                    float finalLuminance = FlashLightClamp.clampLuminance(FlashLightRenderer.luminance);
                    float finalRange = FlashLightClamp.clampRange(FlashLightRenderer.range);
                    float finalAngle = FlashLightClamp.clampAngle(FlashLightRenderer.angle);
                    for (PostPass pass : passes) {
                        pass.getEffect().safeGetUniform("InversePVMat").set(inverseModelViewMat.mul(inversePerspectiveProjMat));
                        pass.getEffect().safeGetUniform("To").set(direction);
                        pass.getEffect().safeGetUniform("Angle").set((float) Math.toRadians(finalAngle));
                        pass.getEffect().safeGetUniform("Range").set(finalRange);
                        pass.getEffect().safeGetUniform("Luminance").set(finalLuminance);
                        pass.getEffect().safeGetUniform("MinZ").set(Client.isIrisShaderInUse ? 0.75f : 0f);
                        pass.getEffect().safeGetUniform("Mode").set(1);
                        pass.getEffect().safeGetUniform("TexelSize").set(1.0f / lastWidth, 1.0f / lastHeight);
                    }
                    float partialTicks = event.getPartialTick().getRealtimeDeltaTicks();
                    flashlight.process(partialTicks, (p) -> {
                        if (Minecraft.useShaderTransparency()) {
                            for(int i = 0; i < p.auxAssets.size(); ++i) {
                                String s = p.auxNames.get(i);
                                if ("DiffuseDepthSampler".equals(s)) {
                                    int colorTextureId = FabulousDepthTextureHandler.getDepthTextureId();
                                    p.getEffect().setSampler(s, () -> colorTextureId);
                                    p.getEffect().safeGetUniform("AuxSize" + i).set((float) p.auxWidths.get(i), (float) p.auxHeights.get(i));
                                    break;
                                }
                            }
                        }
                    });
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                    doEffect = false;
                    luminance = 0;
                    range = 0;
                    angle = 0;
                }
            }
        }
    }
}
