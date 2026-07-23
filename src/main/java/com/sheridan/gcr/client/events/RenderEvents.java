package com.sheridan.gcr.client.events;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.WeaponStatus;
import com.sheridan.gcr.client.animation.CameraAnimationHandler;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.render.HardCodeAnimationHandler;
import com.sheridan.gcr.client.render.IrisExtendRT;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.modules.IScope;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.ArrayDeque;
import java.util.Deque;

@OnlyIn(Dist.CLIENT)
public class RenderEvents {
    private static final Deque<Runnable> DELAYED_ENTITY_RENDER_TASKS = new ArrayDeque<>();
    private static final Deque<Runnable> DELAYED_RENDER_TASKS = new ArrayDeque<>();
    private static Runnable finalStageDelayedRenderTask;
    public static ResourceLocation CROSSHAIR_TEXTURE = GCR.RL("textures/gui/crosshair/crosshair.png");

    @SubscribeEvent
    public static void onRenderTickStart(RenderFrameEvent.Pre event) {
        Client.MAX_SHADER_TEXTURES = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        Client.isUsingIrisShader = IrisCompat.isShaderPackInUse();
        Utils.setUpStencil();
        DrawHolsterHandler.get().onRenderTick(Minecraft.getInstance().gameRenderer.itemInHandRenderer);
        Client.getGunRenderer().renderTickPre(event.getPartialTick().getRealtimeDeltaTicks());
    }

    @SubscribeEvent
    public static void onRenderTickEnd(RenderFrameEvent.Post event) {
        Client.getGunRenderer().renderTickPost(event.getPartialTick().getRealtimeDeltaTicks());
    }

    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            if (Client.WEAPON_STATUS.isHoldingGun()) {
                event.setCanceled(true);
//                if (Minecraft.getInstance().screen instanceof GunModifyScreen) {
//                    return;
//                }
//                if (Client.WEAPON_STATUS.getAimingProgress() <= 0.1f) {
//                    GuiGraphics guiGraphics = event.getGuiGraphics();
//                    RenderSystem.enableBlend();
//                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//                    int centerX = (int) ((guiGraphics.guiWidth() - 1) / 2f);
//                    int centerY = (int) ((guiGraphics.guiHeight() - 1) / 2f);
//                    guiGraphics.blit(CROSSHAIR_TEXTURE, centerX, centerY, 0, 0, 2, 2, 2, 2);
//                    RenderSystem.defaultBlendFunc();
//                    RenderSystem.disableBlend();
//                }
            }
        }
    }

    public static void setFinalStageDelayedRenderTask(Runnable renderTask) {
        finalStageDelayedRenderTask = renderTask;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Client.FIRST_PERSON_PROJECTION_MAT.set(new Matrix4f(RenderSystem.getProjectionMatrix()));
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            if (Client.WEAPON_STATUS.isHoldingGun()) {
                HardCodeAnimationHandler.getInstance().updateOnRenderTick(event.getPartialTick());
            }
            ItemStack itemStack = event.getItemStack();
            if (itemStack.getItem() instanceof GunItem) {
                if (Client.isUsingIrisShader && !IrisCompat.isRenderingShadowPass()) {
                    ShaderInstance shader = BufferedBoneMeshModel.getShader();
                    if (shader != null) {
                        shader.apply();
                        IrisExtendRT.ensureMuzzleAttachment();
                        shader.clear();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void checkIrisExtensions(RenderFrameEvent.Pre event) {
        if (Client.isUsingIrisShader) {
            RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
            int width = mainRenderTarget.width;
            int height = mainRenderTarget.height;
            IrisExtendRT.preFrameCheck(width, height);
        }
    }

    public static void addDelayedEntityRenderTask(Runnable task) {
        DELAYED_ENTITY_RENDER_TASKS.add(task);
    }

    public static void addDelayedRenderTask(Runnable task) {
        DELAYED_RENDER_TASKS.add(task);
    }

    @SubscribeEvent
    public static void handleCameraAnimation(ViewportEvent.ComputeCameraAngles event) {
        CameraAnimationHandler.INSTANCE.apply(event);
        CameraAnimationHandler.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Client.currentStage = event.getStage();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            while (!DELAYED_ENTITY_RENDER_TASKS.isEmpty()) {
                DELAYED_ENTITY_RENDER_TASKS.poll().run();
            }
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            while (!DELAYED_RENDER_TASKS.isEmpty()) {
                DELAYED_RENDER_TASKS.poll().run();
            }
            if (finalStageDelayedRenderTask != null) {
                finalStageDelayedRenderTask.run();
                finalStageDelayedRenderTask = null;
            }
        }
    }

    public static double currentFov = 70;
    @SubscribeEvent
    public static void onGetFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            currentFov = event.getFOV();
        }
    }

    private static Double baseSensitivity = null;
    @SubscribeEvent
    public static void handleScope(ComputeFovModifierEvent event) {
        WeaponStatus status = Client.WEAPON_STATUS;
        Minecraft mc = Minecraft.getInstance();


        if (status.isHoldingGun() && status.isAiming()) {
            Node activeSight = status.getActiveSight();

            if (activeSight != null && activeSight.getModule() instanceof IScope scope) {
                ItemStack itemStack = status.getItemStack();
                IGun gun = status.getGun();

                CompoundTag scopeStates = gun.getNodeStatesTag(itemStack, activeSight.getID());

                float rate = scope.getRate(scopeStates); // [1,+Inf)
                float aimingProgress = status.getAimingProgress();

                float p = aimingProgress * aimingProgress * (3.0f - 2.0f * aimingProgress);
                float targetModifier = 1.0f / rate;
                float modifier = Mth.lerp(p, 1.0f, targetModifier);

                event.setNewFovModifier(event.getNewFovModifier() * modifier);

                if (baseSensitivity == null) {
                    baseSensitivity = mc.options.sensitivity().get();
                }

                double newSensitivity = baseSensitivity * modifier;
                mc.options.sensitivity().set(newSensitivity);
            } else {
                if (baseSensitivity != null) {
                    mc.options.sensitivity().set(baseSensitivity);
                    baseSensitivity = null;
                }
                event.setNewFovModifier(1);
            }
        } else {
            if (baseSensitivity != null) {
                mc.options.sensitivity().set(baseSensitivity);
                baseSensitivity = null;
            }
            if (status.isHoldingGun()) {
                event.setNewFovModifier(1);
            }
        }
    }

//    @SubscribeEvent
//    public static void fpsMeter(RenderGuiEvent.Post event) {
//        if (!GCR.IS_DEVELOPMENT) {
//            return;
//        }
//        GuiGraphics guiGraphics = event.getGuiGraphics();
//        guiGraphics.drawString(Minecraft.getInstance().font, "FPS: " + Minecraft.getInstance().getFps(), 10, 10, 0xFFFFFF);
//    }

}
