package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
/**
 * 对于全局动画的硬编码实现
 * */
@OnlyIn(Dist.CLIENT)
public class HardCodeAnimationHandler implements IGlobalAnimationHandler {
    protected static IGlobalAnimationHandler INSTANCE;
    private static final MovingInertialHandler MOVING_INERTIAL_HANDLER = new MovingInertialHandler();
    private static final float PI = (float) Math.PI;
    /** 闲置/呼吸动画的计时器 (跨帧保持状态) */
    private float idleProgress = 0;

    private long lastUpdate;
    private float globalScale = 1f;

    private float rxPre, ryPre, rzPre, txPre, tyPre, tzPre;
    private float rxPost, ryPost, rzPost, txPost, tyPost, tzPost;

    float walkDist;
    float bob;
    float walkSwing;

    public static void _debugReloadInstance(IGlobalAnimationHandler hardCodeAnimationHandler) {
        if (!GCR.IS_DEVELOPMENT) {
            return;
        }
        INSTANCE = hardCodeAnimationHandler;
    }

    public void applyTransformPre(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player) {
        calcIdle();
        calcMoveInertial();
        calcHeadSway(partialTicks, player);
        calcMoveBob(partialTicks, player);
        calcEquipTranslate(gun, partialTicks, player);

        finalApplyPre(poseStack);
    }

    private void calcEquipTranslate(IGun gun, float partialTicks, LocalPlayer player) {
        float equipProgress = DrawHolsterHandler.get().getEquipProgress(partialTicks);
        float trans = 1 - equipProgress;
        trans *= trans;
        tyPre -= trans * 2f;
    }

    @Override
    public void applyTransformPost(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player) {
        float scale = 1 - Client.getAimingProgress();
        //System.out.println(SprintingHandler.INSTANCE.getSprintingProgress());
        //calcSprinting(partialTicks, gun);
        finalApplyPost(poseStack, scale, scale);
    }

    private void finalApplyPost(PoseStack poseStack, float rScale, float tScale) {
        if (rScale < 1e-5 && tScale < 1e-5) {
            return;
        }
        float f = 1 - Client.getAimingProgress() * 0.3f;
        f *= rScale;
        if (tScale > 1e-5) {
            poseStack.translate(txPost * tScale, tyPost * tScale, tzPost * tScale);
        }
        poseStack.mulPose(new Quaternionf().rotateXYZ(rxPost * f, ryPost * f, rzPost * f * f));
        rxPost = 0;
        ryPost = 0;
        rzPost = 0;
        txPost = 0;
        tyPost = 0;
        tzPost = 0;
    }

    private void finalApplyPre(PoseStack poseStack) {
        float aimingProgress = Client.getAimingProgress();
        float f = 1 - aimingProgress * 0.65f;
        float f2 = 1 - aimingProgress * 0.35f;
        poseStack.translate(txPre * f, tyPre * f, tzPre * f);
        poseStack.mulPose(new Quaternionf().rotateXYZ(rxPre, ryPre * f2, rzPre * f2));
        rxPre = 0;
        ryPre = 0;
        rzPre = 0;
        txPre = 0;
        tyPre = 0;
        tzPre = 0;
        if (Client.isAiming()) {
            finalApplyPost(poseStack, aimingProgress, 0);
        }
    }

    private void calcSprinting(float partialTicks, IGun gun) {
        DisplayData displayData = gun.getDisplayData();
        if (displayData == null) {
            return;
        }
        float sprintingProgress = SprintingHandler.INSTANCE.getSprintingProgress(partialTicks);
        if (sprintingProgress != 0) {
            float smooth = sprintingProgress * sprintingProgress * (3f - 2f * sprintingProgress);
            float easeOut = 1f - (1f - sprintingProgress) * (1f - sprintingProgress);
            float easeIn = sprintingProgress * sprintingProgress;

            float[] sprintingTranslate = displayData.getSprintingTranslate();

            float tx = Mth.lerp(sprintingProgress, 0, sprintingTranslate[0]);
            float ty = Mth.lerp(sprintingProgress, 0, sprintingTranslate[1]);
            float tz = Mth.lerp(sprintingProgress, 0, sprintingTranslate[2]);

            float rx = Mth.lerp(sprintingProgress, 0, sprintingTranslate[3]);
            float ry = Mth.lerp(sprintingProgress, 0, sprintingTranslate[4]);
            float rz = Mth.lerp(sprintingProgress, 0, sprintingTranslate[5]);

            txPost += tx;
            tyPost += ty;
            tzPost += tz;
            rxPost += rx;
            ryPost += ry;
            rzPost += rz;


        }
    }

    private void calcMoveInertial() {
        float f = globalScale * globalScale;
        tyPre -= MOVING_INERTIAL_HANDLER.getYOffset() * f * 0.5f;
        rzPost -= MOVING_INERTIAL_HANDLER.getXOffset() * (0.35f + f * 0.55f);
    }

    private void calcIdle() {
        float idle = idleProgress * 1.5f;
        float f = 1 - Client.getAimingProgress() * 0.75f;
        float sin = Mth.sin(idle);
        float cos = Mth.cos(idle * 0.5f);
        rxPre += Mth.sin(idleProgress * 0.75f) * 0.005f * globalScale * f;
        ryPost += cos * 0.008f * globalScale * f;
        txPre -= cos * 0.005f * globalScale * f;
        tyPre += sin * 0.01f * globalScale * f;
        tzPre += sin * 0.0015f * globalScale * f;
    }

    public void calcMoveBob(float partialTicks, LocalPlayer player) {
        walkSwing = Math.min(player.walkDist - player.walkDistO, 0.25F);
        walkDist = -(player.walkDist + walkSwing * partialTicks) * PI;
        bob = Mth.lerp(partialTicks, player.oBob, player.bob) * globalScale;

        txPre += bob * Mth.sin(walkDist) * 0.07f;
        tyPre -= bob * (0.82f - Math.abs(Mth.cos(walkDist - PI * 0.1f))) * 0.35f;
        tzPost += bob * Math.abs(Mth.sin(walkDist - PI * 0.013F)) * 0.014f + bob * 0.18f;
        rxPost += (Math.abs(Mth.cos(walkDist - PI * 0.02F * bob) * bob) - Mth.clamp(walkDist * bob, 0, 0.4f)) * 0.175f;
        rzPost -= Mth.clamp(Mth.sin(walkDist * 2f) * 1.1f, -1f, 1f) * bob * 0.06f;
    }

    public void calcHeadSway(float partialTicks, LocalPlayer player) {
        float xBob = Mth.lerp(partialTicks, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTicks, player.yBobO, player.yBob);

        float pitch = player.getViewXRot(partialTicks);

        float xSwing = Mth.clamp((pitch - xBob) * 0.045F, -4f, 2f);
        float ySwing = (player.getViewYRot(partialTicks) - yBob) * 0.075F;

        rxPre += (float) Math.toRadians(xSwing);
        ryPre += (float) Math.toRadians(ySwing);
        rzPost -= ryPre * 0.6f;
    }

    public void clientTick(LocalPlayer player) {
        MOVING_INERTIAL_HANDLER.handle(player);
    }

    @Override
    public void update(float delta) {
        MOVING_INERTIAL_HANDLER.update();
    }

    public void updateOnRenderTick(float particleTicks) {
        long now = System.nanoTime();
        if (lastUpdate == 0) {
            lastUpdate = now - 1_000_000;
        }
        float delta = (now - lastUpdate) / 1e9f;
        lastUpdate = now;
        idleProgress += delta;
        if (idleProgress > PI * 2.66666666666f) {
            idleProgress = 0;
        }
        globalScale = Mth.clamp(Client.distFromLastShoot() * 5, 0.8f, 1f);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.isSprinting()) {
                globalScale *= 1.2f;
            }
        }
    }

    static {
        INSTANCE = new HardCodeAnimationHandler();
    }

    public static IGlobalAnimationHandler getInstance() {
        return INSTANCE;
    }

    @OnlyIn(Dist.CLIENT)
    private static class MovingInertialHandler {
        public static final float OFFSET_SCALE = 0.2f;
        public static final float MAX_OFFSET = 0.5f;
        public static final float MIN_OFFSET = -0.3f;
        private volatile float lastPlayerYSpeed;
        private volatile float YA;
        private float yVelocity;
        private float xVelocity;
        private float yOffset;
        private float xOffset;
        private volatile float lastPlayerXSpeed;
        private volatile float XA;
        private boolean work;

        public float getYOffset() {
            float offset = yOffset * OFFSET_SCALE;
            if (offset < 0) {
                offset *= 0.8f;
            }
            return Mth.clamp(offset, MIN_OFFSET, MAX_OFFSET);
        }

        public float getXOffset() {
            float offset = xOffset * OFFSET_SCALE;
            return Mth.clamp(offset, MIN_OFFSET, MAX_OFFSET);
        }

        public void handle(LocalPlayer localPlayer) {
            float ySpeed = (float) (localPlayer.getY() - localPlayer.yOld);
            float a = ySpeed - lastPlayerYSpeed;
            float xSpeed = getSidewaysSpeed(localPlayer);
            float xa = xSpeed - lastPlayerXSpeed;
            this.XA = xa;
            if (a < 0) {
                a *= 0.5f;
            }
            this.YA = a;
            if (a != 0 || xa != 0) {
                work = true;
            }
            lastPlayerYSpeed = ySpeed;
            lastPlayerXSpeed = xSpeed;
        }

        private float getSidewaysSpeed(LocalPlayer localPlayer) {
            Vec3 motion = localPlayer.getDeltaMovement();
            float yaw = localPlayer.getYRot();
            float sideX = (float) Math.cos(Math.toRadians(yaw));
            float sideZ = (float) Math.sin(Math.toRadians(yaw));
            return (float) (motion.x * sideX + motion.z * sideZ);
        }

        private void update() {
            if (work) {
                xVelocity += XA * 0.6f;
                yVelocity += YA * 0.7f;
                yOffset += yVelocity * 0.15f;
                xOffset += xVelocity * 0.12f;
                yVelocity -= yOffset * 0.12f;
                xVelocity -= xOffset * 0.12f;
                yVelocity *= 0.7f;
                xVelocity *= 0.85f;
                if ((Math.abs(yVelocity) < 1e-5 && Math.abs(yOffset) < 1e-5 && Math.abs(YA) < 1e-5) &&
                        (Math.abs(xVelocity) < 1e-5 && Math.abs(xOffset) < 1e-5 && Math.abs(XA) < 1e-5))  {
                    yVelocity = 0;
                    yOffset = 0;
                    YA = 0;
                    xVelocity = 0;
                    xOffset = 0;
                    XA = 0;
                    work = false;
                }
            }
        }
    }
}