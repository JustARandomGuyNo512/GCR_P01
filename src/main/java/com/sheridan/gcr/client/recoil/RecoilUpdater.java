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
import org.joml.Matrix4f;
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
    // 持有当前武器的参数引用
    private RecoilData data;
    private float noiseTimerX = (float) (50 + Math.random() * 100);
    private float noiseTimerY = (float) (50 + Math.random() * 100);

    // --- 物理状态 ---
    private final Vector3f gunDisplacement = new Vector3f();
    private final Vector3f gunVelocity = new Vector3f();

    // 内部物理状态拆分
    // 1. 基础发力上抬 (Base Pitch)
    private float basePitchDisplacement = 0f;
    private float basePitchVelocity = 0f;

    // 2. 随机震动与偏航 (Random Pitch & Yaw) - X存储随机垂直(Pitch)，Y存储随机水平(Yaw)
    private final Vector3f randomAngularDisplacement = new Vector3f();
    private final Vector2f randomAngularVelocity = new Vector2f();

    // 3. 侧倾 (Roll)
    private float rollDisplacement = 0f;
    private float rollVelocity = 0f;

    // 汇总后的总物理角状态（用于向渲染层同步）
    private final Vector3f gunAngularDisplacement = new Vector3f();
    private final Vector3f gunAngularVelocity = new Vector3f();


    private float camUpSpeed = 0f;
    private float camRandomSpeedPitch = 0f;
    private float camRandomSpeedYaw = 0f;
    private float camShake = 0f;

    // 渲染相关变量 ...
    private static final double PHYSICS_TICK_NANOS = 10_000_000.0;
    private final Vector3f prevRenderGunDisplacement = new Vector3f();
    private final Vector3f prevRenderGunAngularDisplacement = new Vector3f();
    private final Vector3f currRenderGunDisplacement = new Vector3f();
    private final Vector3f currRenderGunAngularDisplacement = new Vector3f();
    private final ReentrantLock renderStateLock = new ReentrantLock();
    private volatile long lastPhysicsUpdateTime = System.nanoTime();
    private float recoilHeat = 0f;
    private float recoilBackEMA = 0f;
    private float randomSeed = 0f;

    /**
     *物理线程在 update 结束时调用，用于发布状态给渲染线程
     */
    private void publishRenderState() {
        renderStateLock.lock();
        try {
            prevRenderGunDisplacement.set(currRenderGunDisplacement);
            prevRenderGunAngularDisplacement.set(currRenderGunAngularDisplacement);
            currRenderGunDisplacement.set(gunDisplacement);
            currRenderGunAngularDisplacement.set(gunAngularDisplacement);
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

        // 线性与ADS加成参数
        float motionAdsModifierStiff = Mth.lerp(aimingFactor, 1.0f, controller.motionAdsModifierStiff());
        float motionAdsModifierDamp = Mth.lerp(aimingFactor, 1.0f, controller.motionAdsModifierDamp());

        float k_lin_z = controller.linearZStiffness() * motionAdsModifierStiff;
        float c_lin_z = controller.linearZDamping() * motionAdsModifierDamp;

        float aimingRotFactorStiff = Mth.lerp(aimingFactor, 1.0f, controller.rotAdsModifierStiff());
        float aimingRotFactorDamp = Mth.lerp(aimingFactor, 1.0f, controller.rotAdsModifierDamp());

        // 1. 线性位移计算
//        float vz = gunVelocity.z;
//        float z  = gunDisplacement.z;
//        vz = (vz - k_lin_z * z * dt) / (1.0f + c_lin_z * dt);
//        z += vz * dt;
//        gunVelocity.z = vz;
//        gunDisplacement.z = z;

        gunVelocity.x = (gunVelocity.x - k_lin_z * gunDisplacement.x * dt) / (1.0f + c_lin_z * dt);
        gunVelocity.y = (gunVelocity.y - k_lin_z * gunDisplacement.y * dt) / (1.0f + c_lin_z * dt);
        gunVelocity.z = (gunVelocity.z - k_lin_z * gunDisplacement.z * dt) / (1.0f + c_lin_z * dt);
        gunDisplacement.add(gunVelocity.x * dt, gunVelocity.y * dt, gunVelocity.z * dt);

        // 2. 角位移计算（根据状态隔离并独立使用恢复参数）

        // -- 2.1 基础发力上抬 (Base Pitch)
        float k_ang_pitch = controller.pitchStiffness() * aimingRotFactorStiff * recoilControl;
        float c_ang_pitch = controller.pitchDamping()  * aimingRotFactorDamp * recoilControl;
        float torqueBasePitch = -k_ang_pitch * basePitchDisplacement - c_ang_pitch * basePitchVelocity;

        basePitchVelocity += torqueBasePitch * dt;
        basePitchDisplacement += basePitchVelocity * dt;

        // -- 2.2 随机震动与偏航 (Random Pitch & Yaw)
        float k_ang_rand_pitch = controller.randomStiffnessPitch() * aimingRotFactorStiff * recoilControl;
        float c_ang_rand_pitch  = controller.randomDampingPitch() * aimingRotFactorDamp * recoilControl;
        float k_ang_rand_Yaw = controller.randomStiffnessYaw() * aimingRotFactorStiff * recoilControl;
        float c_ang_rand_Yaw  = controller.randomDampingYaw() * aimingRotFactorDamp * recoilControl;
        float torqueRandPitch = -k_ang_rand_pitch * randomAngularDisplacement.x - c_ang_rand_pitch * randomAngularVelocity.x;
        float torqueRandYaw = -k_ang_rand_Yaw * randomAngularDisplacement.y - c_ang_rand_Yaw * randomAngularVelocity.y;

        randomAngularVelocity.add(torqueRandPitch * dt, torqueRandYaw * dt);
        randomAngularDisplacement.add(randomAngularVelocity.x * dt, randomAngularVelocity.y * dt, 0);

        // -- 2.3 侧倾 (Roll)
        float k_ang_roll = controller.rollStiffness() * aimingRotFactorStiff;
        float c_ang_roll = controller.rollDamping() * aimingRotFactorDamp;
        float zFactor = Mth.lerp(-gunDisplacement.z * 6, 1, 1.4f);
        float torqueRoll = (-k_ang_roll * zFactor) * rollDisplacement - c_ang_roll * rollVelocity;

        rollVelocity += torqueRoll * dt;
        rollDisplacement += rollVelocity * dt * zFactor;

        gunAngularDisplacement.set(
                basePitchDisplacement + randomAngularDisplacement.x,
                randomAngularDisplacement.y,
                rollDisplacement
        );
        gunAngularVelocity.set(
                basePitchVelocity + randomAngularVelocity.x,
                randomAngularVelocity.y,
                rollVelocity
        );

        recoilBackEMA = 0.1f * gunDisplacement.z + 0.9f * recoilBackEMA;

        updateRecoilHeat(Client.WEAPON_STATUS.getFireInterval(), recoilControlRatio, 0.8f, 0.082f);
        publishRenderState();
    }

    private float randomNoiseX(float seed) {
        return noise1DX.sample(seed) + (RANDOM.nextFloat() * 0.5f - 0.25f);
    }

    private float randomNoiseY(float seed) {
        return noise1DY.sample(seed) + (RANDOM.nextFloat() * 0.5f - 0.25f);
    }

    public float getRecoilHeat() {
        return recoilHeat;
    }

    @Override
    public void onShoot(Player player) {
        if (data == null) {
            return;
        }

        RecoilImpulse impulse = data.getImpulse();
        applyImpulse(
                impulse.impulseZ(),
                impulse.randomPitch(),
                impulse.randomYaw(),
                impulse.shakePitch(),
                impulse.shakeYaw(),
                impulse.shakeRoll()
        );
    }

    @Override
    public void applyImpulse(float impulseZ, float pitch, float yaw, float shakePitch, float shakeYaw, float shakeRoll) {
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
        float rotLever = impulse.leverArmY() * recoilControlFactor
                * (Mth.clamp(1 - aimingFactorSqr, 0.05f, 1f));

         impulseZ *= Math.max(0, impulseVal);

        float torqueImpulseX = (float) (rotLever * impulseZ * (0.6f + recoilHeatRes * 0.4f) * (0.9f + 0.2f * Math.random()));

        float dynamicRand = (float) (Mth.lerp(recoilHeatRes, impulse.randomStart(), 1f) *
                (2.8f - aimingFactor * 2.65f) *
                stableFactor * Math.sqrt(impulseVal));
        impulseZ *= (float) (0.8f + 0.4f * Math.random());
        float randPitch = randomNoiseX(noiseTimerX) * pitch * dynamicRand;

        randPitch *= 1 - 0.3f * aimingFactorSqr;

        float randPitchCam = randPitch > 0 ? randPitch * 0.7f : randPitch;

        float randYawDir = randomNoiseY(noiseTimerY);
        float randYaw = randYawDir * yaw * dynamicRand;

        // 随机震动
        shakePitch *= (RANDOM.nextBoolean() ? 1 : -1);
        shakeYaw *= (RANDOM.nextBoolean() ? 1 : -1);


        float shakeRollRandomSize = (RANDOM.nextFloat() - 0.5f) * Math.min(1, Math.abs(gunDisplacement.z));
        float rawShakeRoll = -shakeRoll * (1 + shakeRollRandomSize);


        float shakeFactor = 1 - Mth.clamp(-gunDisplacement.z * 6, 0, 0.7f + RANDOM.nextFloat() * 0.2f);

        if (Client.isAiming()) {
            shakeFactor = Mth.lerp(aimingFactor, shakeFactor, -aimingFactor * (RANDOM.nextFloat() + 0.5f));
        }

        float adsShakeFactor = 1 - aimingFactor * (0.9f + RANDOM.nextFloat() * 0.08f);

        float rollVelocityImpulse = rawShakeRoll * shakeFactor * adsShakeFactor;
        float shakeZFactor = Math.min(1.0f - shakeFactor, adsShakeFactor) * adsShakeFactor;
        float rollDisplacementImpulse = rawShakeRoll * shakeZFactor * 0.03f;

        gunVelocity.add(0, data.getImpulse().impulseYOffset() * impulseZ, impulseZ);

        basePitchVelocity += torqueImpulseX;
        randomAngularVelocity.add(randPitch + shakePitch, randYaw + shakeYaw);
        rollVelocity += rollVelocityImpulse;

        rollDisplacement += rollDisplacementImpulse;

        float camImpactScale = 0.0088f + aimingFactor * 0.0062f;
        float camRandomScale = 0.001f + aimingFactor * 0.05f;
        float camImpact = camImpactScale * (torqueImpulseX + impulseZ * (0.6f + aimingFactor * 0.4f));
        float camImpactRandomYaw = randYaw * camRandomScale;
        float camImpactRandomPitch = randPitchCam * camRandomScale;

        float shakeDir = randYawDir > 0 ? 2e-4f : -2e-4f;
        this.camShake = shakeDir * shakeRoll;

        applyCamImpulse(camImpact, camImpactRandomPitch, camImpactRandomYaw, recoilControlFactor, aimingFactor);
        randomSeed = RANDOM.nextFloat();
        lastShoot = System.currentTimeMillis();
    }


    public void updateRecoilHeat(float fireInterval, float controlMod, float recoilControlSpeed, float baseRecoveryRate) {

        float friction = -0.35f * (controlMod - 1) + 0.95f;
        friction = Math.clamp(friction, 0.25f, 0.97f);
        this.camUpSpeed *= friction;
        this.camRandomSpeedPitch *= friction;
        this.camRandomSpeedYaw *= friction;

        if (Math.abs(this.camUpSpeed) < 1e-3f) {
            this.camUpSpeed = 0.0f;
        }

        if (Math.abs(this.camRandomSpeedPitch) < 1e-3f) {
            this.camRandomSpeedPitch = 0.0f;
        }

        if (Math.abs(this.camRandomSpeedYaw) < 1e-3f) {
            this.camRandomSpeedYaw = 0.0f;
        }

        float timeSinceLastShoot = Client.distFromLastShoot();

        if (timeSinceLastShoot > fireInterval + 0.05f) {

            float actualRecoveryRate = Math.max(0.1f, baseRecoveryRate * controlMod);

            this.recoilHeat -= actualRecoveryRate;
            if (this.recoilHeat < 0.0f) {
                this.recoilHeat = 0.0f;
            }
        }
    }

    @Override
    public float distFromLastShoot() {
        return (System.currentTimeMillis() - lastShoot) * 0.001f;
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
        // 1. 计算插值 alpha
        float aimingProgress = Client.getAimingProgress();
        aimingProgress *= aimingProgress;
        long now = System.nanoTime();
        double timeSinceLastUpdate = (double) (now - lastPhysicsUpdateTime);
        float alpha = (float) Math.max(0.0, Math.min(1.0, timeSinceLastUpdate / PHYSICS_TICK_NANOS));
        float recoilHeatRes = getRecoilHeat();
        Vector3f lerpGunDisplacement = new Vector3f();
        Vector3f lerpGunAngular = new Vector3f();

        renderStateLock.lock();
        try {
            prevRenderGunDisplacement.lerp(currRenderGunDisplacement, alpha, lerpGunDisplacement);
            prevRenderGunAngularDisplacement.lerp(currRenderGunAngularDisplacement, alpha, lerpGunAngular);
        } finally {
            renderStateLock.unlock();
        }

        float adsZCompensation = Client.WEAPON_STATUS.getLerpAdsZCompensation(particleTicks);
        float EMAFactor = aimingProgress * recoilHeatRes * recoilBackEMA * adsZCompensation;

        float distFromLastShoot = Client.distFromLastShoot();
        float shakeX = 0;
        float shakeY = 0;
        if (distFromLastShoot < 1f && this.data != null) {
            float scale = 1f + recoilHeatRes * 0.6f;
            scale *= 1 - aimingProgress * 0.8f;
            scale *= data.getImpulse().shake();
            float omega = (1 + recoilHeatRes * 1.5f) * 22;
            float rand = (randomSeed * 0.5f + 0.5f) * recoilHeatRes;
            float halfPI =  (float) (Math.PI * (0.45f + rand * 0.1f));
            shakeX = (float) Utils.dampedOscillation(distFromLastShoot, scale, omega, 0.3f, rand * halfPI * 0.67f);
            shakeY = (float) Utils.dampedOscillation(distFromLastShoot, scale, omega * 1.1f, 0.28f, rand * halfPI);
        }

        Bone handRotPivot = model.getHandRotPivot();

        poseStack.translate(0, 0, handRotPivot.z);
        poseStack.mulPose(new Quaternionf().rotateXYZ(
                -(float) Math.toRadians(lerpGunAngular.x),
                (float) Math.toRadians(lerpGunAngular.y),
                (float) Math.toRadians(lerpGunAngular.z)
        ));
        poseStack.translate(0, 0, -handRotPivot.z);
        //手臂关节不随摄像机位移，补偿位移
        double up = RecoilCameraHandler.getInstance().getUp() * 0.4f * (1 - aimingProgress);
        up = Math.toRadians(up);
        Matrix4f pose = poseStack.last().pose();
        Vector3f translation = pose.getTranslation(new Vector3f());
        float zDist = -(float) (translation.z * (1 - Math.cos(up)));
        float yDist = -(float) (translation.y * Math.sin(up));

        float zBack = Mth.lerp(
                aimingProgress,
                -lerpGunDisplacement.z,
                -lerpGunDisplacement.z * adsZCompensation);
        zBack += EMAFactor * 0.8f;
        poseStack.translate(
                lerpGunDisplacement.x + shakeX,
                lerpGunDisplacement.y + yDist + shakeY * 0.45f,
                zBack + zDist);
    }

    @Override
    public float getGunKickPitch() {
        return currRenderGunAngularDisplacement.x;
    }

    @Override
    public float getGunKickYaw() {
        return currRenderGunAngularDisplacement.y;
    }

    @Override
    public void setRecoilData(RecoilData data) {
        this.data = data;

        this.noiseTimerX = (float) (Math.random() * 200);
        this.noiseTimerY = (float) (Math.random() * 200);

        // 切换武器时重置状态
        this.basePitchDisplacement = 0f;
        this.basePitchVelocity = 0f;
        this.randomAngularDisplacement.set(0f, 0f, 0f);
        this.randomAngularVelocity.set(0f, 0f);
        this.rollDisplacement = 0f;
        this.rollVelocity = 0f;
        this.gunAngularDisplacement.set(0f, 0f, 0f);
        this.gunAngularVelocity.set(0f, 0f, 0f);
    }

    @Override
    public RecoilData getRecoilData() {
        return data;
    }


    @Override
    public float getCamShakeZ() {
        return this.camShake;
    }

    Vector2f cameraSpeed = new Vector2f();
    @Override
    public Vector2f getCameraSpeed() {
        cameraSpeed.set(camUpSpeed + camRandomSpeedPitch, camRandomSpeedYaw);
        return cameraSpeed;
    }
}