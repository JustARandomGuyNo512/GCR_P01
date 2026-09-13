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
import net.minecraft.world.entity.player.Player;
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
    private float aimingProgress = 0;

    private long lastUpdate;
    private float globalScale = 1f;
    private float idleScale = 1f;
    private float moveInertialScale = 1f;

    // ===== 射击抑制(globalScale 抵抗)参数 =====
    /** 每一发子弹叠加的抑制冲击量 */
    private static final float SHOOT_RESIST_KICK = 0.35f;
    /** 抑制量的连续泄漏(停火恢复)时间常数(秒) */
    private static final float SHOOT_RESIST_RELEASE_TAU = 0.4f;
    /** 输出平滑时间常数(秒)：把逐发离散的冲击摊开成连续的 globalScale 变化 */
    private static final float SHOOT_RESIST_SMOOTH_TAU = 0.06f;
    /** 抑制量最大强度：globalScale */
    private static final float SHOOT_RESIST_STRENGTH = 0.5f;
    /** 单帧最大积分步长(秒)，防止卡顿/切枪后一帧内突变 */
    private static final float MAX_INTEGRATION_STEP = 0.35f;
    /** 射击冲击积累量 [0, 1] */
    private float shootKick = 0f;
    /** 平滑后的射击抑制量 [0, 1]，用于最终缩放 */
    private float shootResist = 0f;
    /** 上一帧的"距上次射击时间"，用于检测新的一发 */
    private float lastShootDist = 0f;

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
        float scale = 1 - aimingProgress;
        calcSprinting(partialTicks, gun, player);
        finalApplyPost(poseStack, scale, scale);
    }

    private void finalApplyPost(PoseStack poseStack, float rScale, float tScale) {
        if (rScale < 1e-5 && tScale < 1e-5) {
            return;
        }
        float f = 1 - aimingProgress * 0.3f;
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
    float sprintingStartSwing = -114514f;
    private void calcSprinting(float partialTicks, IGun gun, Player player) {
        DisplayData displayData = gun.getDisplayData();
        if (displayData == null) {
            return;
        }
        float sprintingProgress = SprintingHandler.INSTANCE.getSprintingProgress(partialTicks);

        if (sprintingProgress != 0) {
            float smooth = sprintingProgress * sprintingProgress * (3f - 2f * sprintingProgress);
            float easeIn = sprintingProgress * sprintingProgress;

            float[] sprintingTranslate = displayData.getSprintingTranslate();

            float tx = Mth.lerp(smooth, 0, sprintingTranslate[0]);
            float ty = Mth.lerp(easeIn, 0, sprintingTranslate[1]);
            float tz = Mth.lerp(easeIn, 0, sprintingTranslate[2]);

            float rx = Mth.lerp(smooth, 0, sprintingTranslate[3]);
            float ry = Mth.lerp(easeIn, 0, sprintingTranslate[4]);
            float rz = Mth.lerp(sprintingProgress, 0, sprintingTranslate[5]);

            txPost += tx;
            tyPost += ty;
            tzPost += tz;
            rxPost += rx;
            ryPost += ry;
            rzPost += rz;

            walkSwing = Math.min(player.walkDist - player.walkDistO, 0.25F);
            walkDist = -(player.walkDist + walkSwing * partialTicks) * PI;
            if (sprintingStartSwing == -114514f) {
                sprintingStartSwing = walkDist + PI;
            }
            walkDist -= sprintingStartSwing;
            bob = Mth.lerp(partialTicks, player.oBob, player.bob) * sprintingProgress * 3f;


            float sin = Mth.sin(walkDist) * bob;
            float cos = Mth.cos(walkDist) * bob;

            rxPost -= Math.abs(cos) * 0.5f;
            rxPost += Mth.sin(walkDist * 2f) * 0.15f * bob;
            ryPost -= sin * 0.5f;

            tyPost -= Math.abs(Mth.cos(walkDist - PI * 0.035F * Math.max(bob, 1.0F)) * bob);
            tyPost += 0.3f * bob;
            txPost += sin * 1.25f;

        } else {
            sprintingStartSwing = -114514f;
        }
    }

    private void calcMoveInertial() {
        float f = moveInertialScale * moveInertialScale;
        tyPre -= MOVING_INERTIAL_HANDLER.getYOffset() * f * 0.5f;
        rzPost -= MOVING_INERTIAL_HANDLER.getXOffset() * (0.35f + f * 0.55f);
    }

    private void calcIdle() {
        float idle = idleProgress * 1.5f;
        float f = 1 - aimingProgress * 0.75f;
        float sin = Mth.sin(idle);
        float cos = Mth.cos(idle * 0.5f);
        rxPre += Mth.sin(idleProgress * 0.75f) * 0.005f * idleScale * f;
        ryPost += cos * 0.009f * idleScale * f;
        txPre -= cos * 0.005f * idleScale * f;
        tyPre += sin * 0.011f * idleScale * f;
        tzPre += sin * 0.0015f * idleScale * f;
    }

    public void calcMoveBob(float partialTicks, LocalPlayer player) {
        walkSwing = Math.min(player.walkDist - player.walkDistO, 0.25F);
        walkDist = -(player.walkDist + walkSwing * partialTicks) * PI;
        bob = Mth.lerp(partialTicks, player.oBob, player.bob) * globalScale;

        txPre += bob * Mth.sin(walkDist) * 0.07f;
        tyPre -= bob * (0.5f - Math.abs(Mth.cos(walkDist - PI * 0.1f))) * 0.35f;
        tzPost += bob * Math.abs(Mth.sin(walkDist - PI * 0.013F)) * 0.014f + bob * 0.18f;
        rxPost += (Math.abs(Mth.cos(walkDist - PI * 0.02F * bob)) - Mth.clamp(- walkDist * bob, 0, 0.3f)) * bob * 0.175f;

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
        aimingProgress = Client.getAimingProgress(particleTicks);
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
        globalScale = calcShootResistance(delta);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.isSprinting()) {
                globalScale *= 1.2f;
            }
        }
        moveInertialScale = globalScale;
        idleScale = 1;
        float sprintingProgress = 1 - SprintingHandler.INSTANCE.getSprintingProgress(particleTicks);
        globalScale *= sprintingProgress;
        idleScale *= sprintingProgress;
    }

    private float calcShootResistance(float delta) {
        float dt = Mth.clamp(delta, 0f, MAX_INTEGRATION_STEP);

        // 距上次射击的时间只会在开火时回退，借此检测新的一发
        float dist = Client.distFromLastShoot();
        if (dist < lastShootDist - 1e-3f) {
            shootKick = Math.min(1f, shootKick + SHOOT_RESIST_KICK);
        }
        lastShootDist = dist;

        // 连续泄漏：停火后抑制量按指数缓慢恢复
        shootKick *= (float) Math.exp(-dt / SHOOT_RESIST_RELEASE_TAU);
        if (shootKick < 1e-5f) {
            shootKick = 0f;
        }

        // 输出平滑：即使冲击是逐发叠加的，globalScale 也不会瞬间跳变
        shootResist += (shootKick - shootResist) * (1f - (float) Math.exp(-dt / SHOOT_RESIST_SMOOTH_TAU));
        shootResist = Mth.clamp(shootResist, 0f, 1f);

        // smoothstep 让接近上限时变化更平缓，最终缩放落在 (0.1, 1]
        float shaped = shootResist * shootResist * (3f - 2f * shootResist);
        return 1f - SHOOT_RESIST_STRENGTH * shaped;
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