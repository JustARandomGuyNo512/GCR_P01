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
import com.sheridan.gcr.client.render.events.GcrHipfireDot;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
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
    public static void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Client.MAX_SHADER_TEXTURES = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        Client.isUsingIrisShader = IrisCompat.isShaderPackInUse();
        Utils.setUpStencil();
        DrawHolsterHandler.get().onRenderTick(Minecraft.getInstance().gameRenderer.itemInHandRenderer);
        Client.getGunRenderer().renderTickPre(event.renderTickTime);
    }

    @SubscribeEvent
    public static void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Client.getGunRenderer().renderTickPost(event.renderTickTime);
    }

    /**
     * Replace only the vanilla crosshair overlay for the equipped GCR weapon.
     *
     * Forge's overlay id is a ResourceLocation, NOT the VanillaGuiOverlay enum
     * instance. Comparing those two types made the old cancellation dead code.
     * Draw in this same guaranteed overlay event instead of relying on a
     * separate RenderGuiEvent.Post callback that may be covered by other HUD.
     */
    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiOverlayEvent.Pre event) {
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * Post-HUD drawing prevents an overlay canceled in Pre from swallowing
     * the replacement reticle. Flush explicitly so the 2px dot reaches the
     * screen after any other mod's buffered HUD decorations.
     */
    @SubscribeEvent
    public static void onRenderHipfireDot(RenderGuiEvent.Post event) {
        if (GcrHipfireDot.draw(event.getGuiGraphics(), event.getPartialTick())) {
            event.getGuiGraphics().flush();
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
    public static void checkIrisExtensions(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
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
