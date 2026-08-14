package com.sheridan.gcr.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.modular.IGunModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.SplittableRandom;
import java.util.concurrent.locks.ReentrantLock;

@OnlyIn(Dist.CLIENT)
public class RecoilUpdater implements IRecoilUpdater {
    private static final SplittableRandom RANDOM = new SplittableRandom(System.currentTimeMillis());
    private static final SmoothNoise1D noise1DX = new SmoothNoise1D((long) (3000 + Math.random() * 1000));
    private static final SmoothNoise1D noise1DY = new SmoothNoise1D((long) (3000 + Math.random() * 1000));

    private long lastShoot = 0;
    private RecoilData data;

    private float noiseTimerX = (float) (50 + Math.random() * 100);
    private float noiseTimerY = (float) (50 + Math.random() * 100);

    // =========================================================================
    // --- 1. 物理轴/通道封装 ---
    // =========================================================================
    private final RecoilSpring1D zLinear = new RecoilSpring1D();      // 线性位移 Z
    private final RecoilSpring1D basePitch = new RecoilSpring1D();    // 基础 Pitch 抬起
    private final RecoilSpring2D localRot = new RecoilSpring2D();     // 模型本地 Pitch (X) & Yaw (Y) 旋转震动
    private final RecoilSpring2D globalRot = new RecoilSpring2D();    // 全局 Pitch (X) & Yaw (Y) 旋转震动
    private final RecoilSpring2D globalTrans = new RecoilSpring2D();  // 全局 X, Y 模型平移位移
    private final RecoilSpring1D roll = new RecoilSpring1D();         // 枪械侧倾 Roll

    // 汇总物理状态（用于兼容渲染层与同步）
    private final Vector3f gunDisplacement = new Vector3f();
    private final Vector3f gunVelocity = new Vector3f();
    private final Vector3f localAngularDisplacement = new Vector3f();
    private final Vector3f globalAngularDisplacement = new Vector3f();
    private final Vector3f gunAngularVelocity = new Vector3f();

    // --- 镜头后坐力状态 ---
    private float camUpSpeed = 0f;
    private float camRandomSpeedPitch = 0f;
    private float camRandomSpeedYaw = 0f;
    private float camShake = 0f;
    private final Vector2f cameraSpeed = new Vector2f();

    // --- 渲染插值相关变量 ---
    private static final double PHYSICS_TICK_NANOS = 10_000_000.0;
    private final Vector3f prevRenderDisplacement = new Vector3f();
    private final Vector3f prevRenderLocalAngularDisplacement = new Vector3f();
    private final Vector3f prevRenderGlobalAngularDisplacement = new Vector3f();
    private final Vector3f currRenderDisplacement = new Vector3f();
    private final Vector3f currRenderLocalAngularDisplacement = new Vector3f();
    private final Vector3f currRenderGlobalAngularDisplacement = new Vector3f();
    private final ReentrantLock renderStateLock = new ReentrantLock();
    private volatile long lastPhysicsUpdateTime = System.nanoTime();

    private float recoilHeat = 0f;
    private float recoilBackEMA = 0f;
    private float randomSeed = 0f;
    private float randomSeed2 = 0f;

    /**
     * 物理线程在 update 结束时调用，用于发布状态给渲染线程
     */
    private void publishRenderState() {
        renderStateLock.lock();
        try {
            prevRenderDisplacement.set(currRenderDisplacement);
            prevRenderLocalAngularDisplacement.set(currRenderLocalAngularDisplacement);
            prevRenderGlobalAngularDisplacement.set(currRenderGlobalAngularDisplacement);
            currRenderDisplacement.set(gunDisplacement);
            currRenderLocalAngularDisplacement.set(localAngularDisplacement);
            currRenderGlobalAngularDisplacement.set(globalAngularDisplacement);
            lastPhysicsUpdateTime = System.nanoTime();
        } finally {
            renderStateLock.unlock();
        }
    }

    @Override
    public void update(double timeDist) {
        if (data == null) {
            return;
        }
        float dt = (float) timeDist;
        float playerDynamicFactor = Client.WEAPON_STATUS.getPlayerDynamicFactor();
        float aimingFactor = Client.WEAPON_STATUS.getAimingProgress();
        float recoilControlRatio = Client.WEAPON_STATUS.getRecoilControl() * playerDynamicFactor;
        aimingFactor *= aimingFactor;

        float control = Math.max(0, recoilControlRatio);
        float recoilControl = (float) Math.sqrt(control);
        RecoilController controller = data.getRecoilController();

        // 缩放加成参数
        float motionAdsModifierStiff = Mth.lerp(aimingFactor, 1.0f, controller.motionAdsModifierStiff());
        float motionAdsModifierDamp = Mth.lerp(aimingFactor, 1.0f, controller.motionAdsModifierDamp());
        float aimingRotFactorStiff = Mth.lerp(aimingFactor, 1.0f, controller.rotAdsModifierStiff());
        float aimingRotFactorDamp = Mth.lerp(aimingFactor, 1.0f, controller.rotAdsModifierDamp());

        // 1.1 线性位移计算 (Z 轴 - 使用隐式欧拉)
        float k_lin_z = controller.linearZStiffness() * motionAdsModifierStiff;
        float c_lin_z = controller.linearZDamping() * motionAdsModifierDamp;
        zLinear.updateImplicit(dt, k_lin_z, c_lin_z);

        // 1.2 全局平移位移 (X, Y 轴移动)
        float k_mov_global = controller.globalMovStiffness() * motionAdsModifierStiff;
        float c_mov_global = controller.globalMovDamping() * motionAdsModifierDamp;
        globalTrans.updateExplicit(dt, k_mov_global, c_mov_global);

        // 2.1 基础发力上抬 (Base Pitch)
        float k_ang_pitch = controller.pitchStiffness() * aimingRotFactorStiff * recoilControl;
        float c_ang_pitch = controller.pitchDamping() * aimingRotFactorDamp * recoilControl;
        basePitch.updateExplicit(dt, k_ang_pitch, c_ang_pitch);

        // 2.2 模型本地旋转 (Local Pitch & Yaw)
        float k_ang_local = controller.localRotStiffness() * aimingRotFactorStiff * recoilControl;
        float c_ang_local = controller.localRotDamping() * aimingRotFactorDamp * recoilControl;
        localRot.updateExplicit(dt, k_ang_local, c_ang_local);

        // 2.3 全局旋转 (Global Pitch & Yaw)
        float k_ang_global = controller.globalRotStiffness() * aimingRotFactorStiff * recoilControl;
        float c_ang_global = controller.globalRotDamping() * aimingRotFactorDamp * recoilControl;
        globalRot.updateExplicit(dt, k_ang_global, c_ang_global);

        // 2.4 侧倾 (Roll)
        float k_ang_roll = controller.rollStiffness() * aimingRotFactorStiff;
        float c_ang_roll = controller.rollDamping() * aimingRotFactorDamp;
        float zFactor = Mth.lerp(-zLinear.getDisplacement() * 8, 1, 1.5f);
        roll.updateExplicit(dt, k_ang_roll * zFactor, c_ang_roll);

        // 3. 汇总物理总向量 (X, Y 平移 + Z 线性后坐)
        gunDisplacement.set(
                0,//globalTrans.getDisplacementX(),
                0,//globalTrans.getDisplacementY(),
                zLinear.getDisplacement()
        );
        gunVelocity.set(
                globalTrans.getVelocityX(),
                globalTrans.getVelocityY(),
                zLinear.getVelocity()
        );

        localAngularDisplacement.set(
                basePitch.getDisplacement() + localRot.getDisplacementX(),
                localRot.getDisplacementY(),
                roll.getDisplacement()
        );

        globalAngularDisplacement.set(
                globalRot.getDisplacementX(),
                globalRot.getDisplacementY(),
                0
        );

        gunAngularVelocity.set(
                basePitch.getVelocity() + localRot.getVelocityX() + globalRot.getVelocityX(),
                localRot.getVelocityY() + globalRot.getVelocityY(),
                roll.getVelocity()
        );

        recoilBackEMA = 0.1f * zLinear.getDisplacement() + 0.9f * recoilBackEMA;

        updateRecoilHeat(Client.WEAPON_STATUS.getFireInterval(), recoilControlRatio, 0.8f, 0.082f);
        publishRenderState();
    }

    @Override
    public void onShoot(Player player) {
        if (data == null) {
            return;
        }

        RecoilImpulse impulse = data.getImpulse();
        applyImpulse(
                impulse.impulseZ(),
                impulse.randomLocalPitch(),
                impulse.randomLocalYaw(),
                impulse.randomGlobalPitch(),
                impulse.randomGlobalYaw(),
                impulse.shakePitch(),
                impulse.shakeYaw(),
                impulse.shakeRoll()
        );
    }

    public void applyImpulse(float impulseZ, float localPitch, float localYaw, float globalPitch, float globalYaw, float shakePitch, float shakeYaw, float shakeRoll) {
        if (data == null) {
            return;
        }

        float playerDynamicFactor = Client.WEAPON_STATUS.getPlayerDynamicFactor();
        float stability = Client.WEAPON_STATUS.getStability() * playerDynamicFactor;
        float impulseVal = Client.WEAPON_STATUS.getImpulse();
        float recoilControl = Client.WEAPON_STATUS.getRecoilControl() * playerDynamicFactor;

        float stableFactor = 1.0f / stability;
        float recoilControlFactor = 1.0f / recoilControl;
        float recoilHeatRes = getRecoilHeat();

        float delta = Math.min(distFromLastShoot(), 1.0f) * 22f;
        this.noiseTimerX += delta;
        this.noiseTimerY += delta;

        float aimingFactor = Client.getAimingProgress();
        float aimingFactorSqr = aimingFactor * aimingFactor;
        RecoilImpulse impulse = data.getImpulse();
        float rotLever = impulse.rotPitch() * recoilControlFactor * (Mth.clamp(1 - aimingFactorSqr, 0.05f, 1f));

        impulseZ *= Math.max(0, impulseVal);

        float torqueImpulseX = (float) (rotLever * impulseZ * (0.6f + recoilHeatRes * 0.4f) * (0.9f + 0.2f * Math.random()));

        float dynamicRand = (float) (Mth.lerp(recoilHeatRes, impulse.randomStart(), 1f) *
                (2.8f - aimingFactor * 2.65f) *
                stableFactor * Math.sqrt(impulseVal));
        impulseZ *= (float) (0.8f + 0.4f * Math.random());

        // 噪声采样
        float noiseX = randomNoiseX(noiseTimerX);
        float noiseY = randomNoiseY(noiseTimerY);

        // 1. 本地旋转扰动 (Local Pitch & Yaw)
        float randLocalPitch = noiseX * localPitch * dynamicRand * (1 - 0.3f * aimingFactorSqr);
        float randLocalYaw = noiseY * localYaw * dynamicRand;

        // 2. 全局旋转扰动与平移扰动 (使用 globalPitch, globalYaw)
        float randGlobalPitch = noiseX * globalPitch * dynamicRand * (1 - 0.3f * aimingFactorSqr);
        float randGlobalYaw = noiseY * globalYaw * dynamicRand;

        // 3. 随机震动方向
        shakePitch *= (RANDOM.nextBoolean() ? 1 : -1);
        shakeYaw *= (RANDOM.nextBoolean() ? 1 : -1);

        float shakeRollRandomSize = (RANDOM.nextFloat() - 0.5f) * Math.min(1, Math.abs(zLinear.getDisplacement()));
        float rawShakeRoll = -shakeRoll * (1 + shakeRollRandomSize);

        float shakeFactor = 1 - Mth.clamp(-zLinear.getDisplacement() * 8, 0, 0.6f + RANDOM.nextFloat() * 0.2f);
        if (Client.isAiming()) {
            shakeFactor = Mth.lerp(aimingFactor, shakeFactor, -aimingFactor * (RANDOM.nextFloat() + 0.5f));
        }

        float adsShakeFactor = 1 - aimingFactor * (0.9f + RANDOM.nextFloat() * 0.08f);

        float rollVelocityImpulse = rawShakeRoll * shakeFactor * adsShakeFactor;
        float shakeZFactor = Math.min(1.0f - shakeFactor, adsShakeFactor) * adsShakeFactor;
        float rollDisplacementImpulse = rawShakeRoll * shakeZFactor * 0.025f;

        // --- 应用脉冲给各个封装好的轴 ---
        zLinear.addVelocity(impulseZ);
        basePitch.addVelocity(torqueImpulseX);
        localRot.addVelocity(shakePitch, shakeYaw);
        globalRot.addVelocity(randGlobalPitch, randGlobalYaw);

        //globalTrans.addVelocity(randGlobalYaw, randGlobalPitch);
        roll.addVelocity(rollVelocityImpulse);
        roll.addDisplacement(rollDisplacementImpulse);

        // 镜头脉冲计算 (汇总本地与全局的随机旋转分量)
        float totalRandPitch = randLocalPitch + randGlobalPitch;
        float totalRandYaw = randLocalYaw + randGlobalYaw;
        float randPitchCam = totalRandPitch > 0 ? totalRandPitch * 0.7f : totalRandPitch;

        float camImpactScale = 0.0088f + aimingFactor * 0.0062f;
        float camRandomScale = 0.001f + aimingFactor * 0.05f;
        float camImpact = camImpactScale * (torqueImpulseX + impulseZ * (0.6f + aimingFactor * 0.4f));
        float camImpactRandomYaw = totalRandYaw * camRandomScale;
        float camImpactRandomPitch = randPitchCam * camRandomScale;

        this.camShake = 1e-4f * shakeRoll;

        applyCamImpulse(camImpact, camImpactRandomPitch, camImpactRandomYaw, recoilControlFactor, aimingFactor);
        randomSeed = RANDOM.nextFloat();
        randomSeed2 = RANDOM.nextFloat();
        lastShoot = System.currentTimeMillis();
    }

    public void updateRecoilHeat(float fireInterval, float controlMod, float recoilControlSpeed, float baseRecoveryRate) {
        float friction = -0.35f * (controlMod - 1) + 0.95f;
        friction = Math.clamp(friction, 0.25f, 0.97f);
        this.camUpSpeed *= friction;
        this.camRandomSpeedPitch *= friction;
        this.camRandomSpeedYaw *= friction;

        if (Math.abs(this.camUpSpeed) < 1e-3f) this.camUpSpeed = 0.0f;
        if (Math.abs(this.camRandomSpeedPitch) < 1e-3f) this.camRandomSpeedPitch = 0.0f;
        if (Math.abs(this.camRandomSpeedYaw) < 1e-3f) this.camRandomSpeedYaw = 0.0f;

        float timeSinceLastShoot = Client.distFromLastShoot();
        if (timeSinceLastShoot > fireInterval + 0.05f) {
            float actualRecoveryRate = Math.max(0.1f, baseRecoveryRate * controlMod);
            this.recoilHeat = Math.max(0.0f, this.recoilHeat - actualRecoveryRate);
        }
    }

    private void applyCamImpulse(float baseImpulse, float randomPitch, float randomYaw, float recoilControlFactor, float aimingProgress) {
        float actualShotsToStable = Math.max(1.5f, Math.min(25.0f, data.getRecoilController().stableDuration() * recoilControlFactor));
        float jumpFactor = Mth.lerp(aimingProgress, 1, Math.clamp(Client.distFromLastJump() * 2f, 0.5f, 1.0f));
        float heatStep = 1.0f / actualShotsToStable;
        this.recoilHeat = Math.min(1.0f, this.recoilHeat + heatStep);

        float heatFactor = (float) Math.pow(this.recoilHeat, 0.75f) * jumpFactor;

        this.camUpSpeed = (1.0f - heatFactor) * baseImpulse;
        this.camRandomSpeedPitch += randomPitch * heatFactor;
        this.camRandomSpeedYaw += randomYaw * heatFactor;
    }

    @Override
    public void applyTransformPost(PoseStack poseStack, boolean aiming, float particleTicks, IGunModel model) {
        float aimingProgress = Client.getAimingProgress();
        aimingProgress *= aimingProgress;
        long now = System.nanoTime();
        double timeSinceLastUpdate = (double) (now - lastPhysicsUpdateTime);
        float alpha = (float) Math.max(0.0, Math.min(1.0, timeSinceLastUpdate / PHYSICS_TICK_NANOS));
        float recoilHeatRes = getRecoilHeat();

        Vector3f lerpGunDisplacement = new Vector3f();
        Vector3f lerpLocalAngular = new Vector3f();
        Vector3f lerpGlobalAngular = new Vector3f();

        renderStateLock.lock();
        try {
            prevRenderDisplacement.lerp(currRenderDisplacement, alpha, lerpGunDisplacement);
            prevRenderLocalAngularDisplacement.lerp(currRenderLocalAngularDisplacement, alpha, lerpLocalAngular);
            prevRenderGlobalAngularDisplacement.lerp(currRenderGlobalAngularDisplacement, alpha, lerpGlobalAngular);
        } finally {
            renderStateLock.unlock();
        }

        float adsZCompensation = Client.WEAPON_STATUS.getLerpAdsZCompensation(particleTicks);
        float EMAFactor = aimingProgress * recoilHeatRes * recoilBackEMA * adsZCompensation;

        float distFromLastShoot = Client.distFromLastShoot();
        float shakeX = 0;
        float shakeY = 0;
        if (distFromLastShoot < 1f) {
            float scale = (1f + recoilHeatRes * 0.6f) * (1 - aimingProgress * 0.7f) * data.getImpulse().shake();
            float omega = (1 + recoilHeatRes * 1.5f) * 22;
            float rand = (randomSeed * 0.5f + 0.5f) * recoilHeatRes;
            float halfPI = (float) (Math.PI * (0.45f + rand * 0.1f));
            shakeX = (float) Utils.dampedOscillation(distFromLastShoot, scale, omega, 0.26f, rand * halfPI * 0.67f);
            shakeY = (float) Utils.dampedOscillation(distFromLastShoot, scale, omega * 1.1f, 0.28f, rand * halfPI);
        }
        Bone handRotPivot = model.getHandRotPivot();
        float z = handRotPivot.z * 2f;
        poseStack.translate(shakeX, shakeY * 0.5f, z);
        poseStack.mulPose(new Quaternionf().rotateXYZ(
                -(float) Math.toRadians(lerpGlobalAngular.x),
                (float) Math.toRadians(lerpGlobalAngular.y),
                (float) Math.toRadians(lerpGlobalAngular.z)
        ));
        poseStack.translate(0, 0, -z);
        float cScale = 0.625f;
        float f1 = Math.signum(randomSeed - 0.5f) * Math.min(randomSeed + 0.5f, 1);
        float f2 = (Math.signum(randomSeed2 - 0.5f) + 0.5f) * Math.min(randomSeed2 + 0.6f, 1);

        float ry = (float) Utils.dampedOscillation(distFromLastShoot, f1 * cScale, 60f, 0.48f, (float) -Math.PI * 0.48f);
        float rx2 = (float) Utils.dampedOscillation(distFromLastShoot, f2 * cScale, 60f, 0.48f, (float) -Math.PI * 0.48f);

        poseStack.mulPose(new Quaternionf().rotateXYZ(
                -(float) Math.toRadians(lerpLocalAngular.x + rx2),
                (float) Math.toRadians(lerpLocalAngular.y + ry),
                (float) Math.toRadians(lerpLocalAngular.z)
        ));

        float zBack = Mth.lerp(
                aimingProgress,
                -lerpGunDisplacement.z,
                -lerpGunDisplacement.z * adsZCompensation);
        zBack += EMAFactor * 0.8f;
        zBack += (float) Utils.dampedOscillation(distFromLastShoot, 0.5f, 28f, 1.5f, (float) -Math.PI * 0.5f);

        poseStack.translate(
                lerpGunDisplacement.x,
                lerpGunDisplacement.y,
                zBack);

    }

    @Override
    public void setRecoilData(RecoilData data) {
        this.data = test; // 保持测试数据设置

        this.noiseTimerX = (float) (Math.random() * 200);
        this.noiseTimerY = (float) (Math.random() * 200);

        // 切换武器时统一重置所有轴
        zLinear.reset();
        basePitch.reset();
        localRot.reset();
        globalRot.reset();
        globalTrans.reset();
        roll.reset();

        gunDisplacement.set(0, 0, 0);
        gunVelocity.set(0, 0, 0);
        localAngularDisplacement.set(0, 0, 0);
        gunAngularVelocity.set(0, 0, 0);
    }

    @Override
    public RecoilData getRecoilData() {
        return data;
    }

    @Override
    public float getGunKickPitch() {
        return currRenderLocalAngularDisplacement.x + currRenderGlobalAngularDisplacement.x;
    }

    @Override
    public float getGunKickYaw() {
        return currRenderLocalAngularDisplacement.y + currRenderGlobalAngularDisplacement.y;
    }

    @Override
    public float getCamShakeZ() {
        return this.camShake;
    }

    @Override
    public Vector2f getCameraSpeed() {
        cameraSpeed.set(camUpSpeed + camRandomSpeedPitch, camRandomSpeedYaw);
        return cameraSpeed;
    }

    @Override
    public float distFromLastShoot() {
        return (System.currentTimeMillis() - lastShoot) * 0.001f;
    }

    public float getRecoilHeat() {
        return recoilHeat;
    }

    private float randomNoiseX(float seed) {
        return noise1DX.sample(seed) + (RANDOM.nextFloat() * 0.5f - 0.25f);
    }

    private float randomNoiseY(float seed) {
        return noise1DY.sample(seed) + (RANDOM.nextFloat() * 0.5f - 0.25f);
    }

    RecoilData test = new RecoilData(
            new RecoilImpulse(
                    5f, 10f,
                    2f, 2f,
                    6, 8,
                    0.15f,
                    200.0f, 3.5f, 3.5f, 0.01f),
            new RecoilController(
                    350f, 40f,
                    150.0f, 11f,
                    100.0f, 8f,
                    120.0f, 10f,
                    120.0f, 10f,
                    900.0f, 18f,
                    2.0f, 1.25f,
                    2.5f, 2f,
                    10f)
    );

    // =========================================================================
    // --- 2. 核心封装类：物理弹簧组件 ---
    // =========================================================================
    /**
     * 单轴物理弹簧组件 (位移 / 单角)
     */
    public static class RecoilSpring1D {
        private float displacement = 0f;
        private float velocity = 0f;

        /**
         * 显式欧拉积分更新
         */
        public void updateExplicit(float dt, float stiffness, float damping) {
            float force = -stiffness * displacement - damping * velocity;
            velocity += force * dt;
            displacement += velocity * dt;
        }

        /**
         * 半隐式/隐式积分更新
         */
        public void updateImplicit(float dt, float stiffness, float damping) {
            velocity = (velocity - stiffness * displacement * dt) / (1.0f + damping * dt);
            displacement += velocity * dt;
        }

        public void addVelocity(float impulse) {
            this.velocity += impulse;
        }

        public void addDisplacement(float delta) {
            this.displacement += delta;
        }

        public void reset() {
            this.displacement = 0f;
            this.velocity = 0f;
        }

        public float getDisplacement() {
            return displacement;
        }

        public float getVelocity() {
            return velocity;
        }
    }

    /**
     * 双轴 (X & Y) 物理弹簧组件，共享刚度与阻尼
     */
    public static class RecoilSpring2D {
        private final Vector2f displacement = new Vector2f(0f, 0f);
        private final Vector2f velocity = new Vector2f(0f, 0f);

        /**
         * 显式欧拉积分更新 (2D 共享弹簧属性)
         */
        public void updateExplicit(float dt, float stiffness, float damping) {
            float fx = -stiffness * displacement.x - damping * velocity.x;
            float fy = -stiffness * displacement.y - damping * velocity.y;

            velocity.x += fx * dt;
            velocity.y += fy * dt;

            displacement.x += velocity.x * dt;
            displacement.y += velocity.y * dt;
        }

        public void addVelocity(float impulseX, float impulseY) {
            this.velocity.add(impulseX, impulseY);
        }

        public void addDisplacement(float deltaX, float deltaY) {
            this.displacement.add(deltaX, deltaY);
        }

        public void reset() {
            this.displacement.set(0f, 0f);
            this.velocity.set(0f, 0f);
        }

        public float getDisplacementX() {
            return displacement.x;
        }

        public float getDisplacementY() {
            return displacement.y;
        }

        public float getVelocityX() {
            return velocity.x;
        }

        public float getVelocityY() {
            return velocity.y;
        }

        public Vector2f getDisplacement() {
            return displacement;
        }

        public Vector2f getVelocity() {
            return velocity;
        }
    }
}