package com.sheridan.gcr.client.recoil.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.modular.IGunModel;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.client.recoil.VisualRecoilMix;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector2f;

import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Demo 级 RecoilUpdater：由 {@link NoiseRecoilData} 的随机参数驱动两层噪声弹簧。
 *
 * <p>核心随机层委托给 {@link NoiseRecoilHandler}，这个类只做 {@link IRecoilUpdater}
 * 的接口适配和绝对时间记录。开发环境里可以临时把 {@code RecoilHandler} 里的
 * {@code new RecoilUpdater()} 换成 {@code new DemoRecoilUpdater()} 做手感对比。
 * {@link #setRecoilData(RecoilData)} 不读取 impulse / controller；只有
 * {@link VisualRecoilMix} 会被 {@link #applyTransformPost} 使用（开火后附加的位移抖动、
 * 旋转抖动和后移 Z）。</p>
 *
 * <p>物理层只做随机旋转：没有确定性抬枪、物理后移和 roll。两层的旋转都会加到
 * {@link #applyTransformPost} 的 poseStack 上：global 层绕 {@link IGunModel#getHandRotPivot()}
 * 转，表现为整把枪被手腕带着慢慢摆；local 层绕模型原点转，表现为枪口附近的高频抖动。
 * 旧 {@link VisualRecoilMix} 只在渲染层额外叠加位移抖动、旋转抖动和后移 Z。
 * 渲染用的角度是物理线程每帧发布的姿态快照插值出来的，不在渲染线程直接读弹簧。</p>
 *
 * <p>注意：同一份噪声目前既通过 {@link #getCameraSpeed()} 驱动相机转角，又通过
 * {@link #applyTransformPost} 加到枪模上。相机 / 枪模最终怎么分账（是否只给枪模、是否按
 * ADS 缩放、global 是否只给相机）还没定，接正式版本时要统一。符号约定沿用旧
 * {@code RecoilUpdater}（pose 的 pitch 取负、yaw 取正）；pose 的 yaw 和玩家 yaw 的正方向
 * 相反，所以 {@link #getGunKickYaw()} 对外要取反，子弹方向才和枪口一致。</p>
 *
 * <p>不是线程安全的；和旧 updater 一样，{@link #update} / {@link #onShoot} 应在
 * {@code ClientWeaponLooper} 的武器线程上串行调用。为了让渲染线程安全读取，{@link #update}
 * 结束时会在一把私有锁里发布不可变的姿态快照，{@link #applyTransformPost} 只读该快照。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DemoRecoilUpdater implements IRecoilUpdater {
    /** 物理推进周期（100Hz），用来算渲染插值的 alpha。 */
    private static final double PHYSICS_TICK_NANOS = 10_000_000.0;
    /** 小于这个角度（度）就当没有扰动，避免空跑矩阵运算。 */
    private static final float POSE_EPSILON_DEG = 1.0e-4f;

    private final NoiseRecoilData data;
    private final NoiseRecoilHandler handler;
    private final Vector2f cameraSpeed = new Vector2f();
    private final SplittableRandom random = new SplittableRandom();

    /**
     * 旧 RecoilData：impulse / controller 不参与运动，但 VisualRecoilMix 会在渲染层使用。
     * 主线程换枪时写、渲染线程读，所以是 volatile（mix 本身是不可变 record）。
     */
    private volatile RecoilData legacyData;

    /** 每一发开火重新随机，供 VisualRecoilMix 的位移/旋转抖动选相位。 */
    private float randomSeed;
    private float randomSeed2;

    // --- 渲染线程读取的状态快照 ---
    private final Object stateLock = new Object();
    /** 两个缓存快照：物理线程原地覆写，避免每个 100Hz 帧 new 一个对象。 */
    private final RecoilSnapshot prevSnapshot = new RecoilSnapshot();
    private final RecoilSnapshot currSnapshot = new RecoilSnapshot();
    /** 渲染线程复用的帧采样，只在渲染线程（手部渲染 / 调试面板）使用。 */
    private final FrameSample frameSample = new FrameSample();
    private volatile long lastPhysicsUpdateNanos = System.nanoTime();

    private double time;
    private double lastShootTime;
    private boolean hasShot;

    /** 持续射击热度（0~1）：每一发累加，停火超过武器间隔 + delay 后回落。 */
    private float recoilHeat;

    public DemoRecoilUpdater() {
        this(defaults());
    }

    public static NoiseRecoilData defaults() {
        return new NoiseRecoilData(
                new NoiseRecoilData.Layer(4, 4, 1.8f, 0.7f, 11f, 1),
                new NoiseRecoilData.Layer(1.2f, 1.2f, 13f, 0.8f, 75f, 0.7f),
                0.12f, 0.35f,
                defaultVisualShake(),
                defaultRecoilHeat());
    }


    public static RecoilHeatProfile defaultRecoilHeat() {
        return new RecoilHeatProfile(13f, 1f, 0.05f, 0.05f, 1.5f, 25f, 0.06f);
    }

    public static VisualShakeProfile defaultVisualShake() {
        return new VisualShakeProfile(
                30f,
                0.26f, 0.28f,
                (float) (-Math.PI * 0.5f),
                (float) (-Math.PI * 0.5f),
                0.1f, 0.1f, 0.1f,
                0.45f, 0.1f, 0.67f,
                3f, 1.0e-4f);
    }

    public DemoRecoilUpdater(NoiseRecoilData data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        this.data = data;
        this.handler = NoiseRecoilHandler.fromData(data);
    }

    @Override
    public void update(double timeDist) {
        float dt = (float) timeDist;
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }

        time += dt;
        handler.update(dt);
        updateRecoilHeat(Client.WEAPON_STATUS.getFireInterval());

        // 兼容旧 RecoilCameraHandler：它把 getCameraSpeed() 当“每次 update 的增量”累加，
        // 所以这里返回 velocity * dt，而不是 velocity 本身。
        cameraSpeed.set(handler.getPitchVelocity() * dt, handler.getYawVelocity() * dt);

        publishSnapshot();
    }

    /**
     * 移植自旧 {@code RecoilUpdater.updateRecoilHeat}：停火超过
     * {@code fireInterval + recoveryDelay} 后，每个 100Hz 帧扣掉
     * {@link RecoilHeatProfile#recoveryRate()}。
     *
     * <p>旧实现就是按“每个 update 调用扣一次”写的，这里保持同样的时间基准；
     * 热度的累加在 {@link #onShoot()} 里完成。</p>
     */
    private void updateRecoilHeat(float fireIntervalSeconds) {
        RecoilHeatProfile profile = data.recoilHeat();
        float sinceShot = hasShot ? (float) (time - lastShootTime) : Float.POSITIVE_INFINITY;
        if (sinceShot > fireIntervalSeconds + profile.recoveryDelay()) {
            recoilHeat = Math.max(0f, recoilHeat - profile.recoveryRate());
        }
    }

    /**
     * 把这一帧的两层弹簧状态打包成不可变快照，发布给渲染线程。
     *
     * <p>除了渲染插值要用的角度，还带上 target、角速度和 activity，让调试面板可以在渲染线程上
     * 安全地显示“逻辑数值”，不用跨线程直接读 handler。</p>
     */
    private void publishSnapshot() {
        // 没开过火时给一个无穷大的 sinceShot，渲染层就不会误用 t=0 的振荡值。
        float sinceShot = hasShot ? (float) (time - lastShootTime) : Float.POSITIVE_INFINITY;
        synchronized (stateLock) {
            prevSnapshot.copyFrom(currSnapshot);
            currSnapshot.globalPitch = handler.getGlobalPitch();
            currSnapshot.globalYaw = handler.getGlobalYaw();
            currSnapshot.localPitch = handler.getLocalPitch();
            currSnapshot.localYaw = handler.getLocalYaw();
            currSnapshot.globalPitchTarget = handler.getGlobalSpring().getPitchTarget();
            currSnapshot.globalYawTarget = handler.getGlobalSpring().getYawTarget();
            currSnapshot.localPitchTarget = handler.getLocalSpring().getPitchTarget();
            currSnapshot.localYawTarget = handler.getLocalSpring().getYawTarget();
            currSnapshot.pitchVelocity = handler.getPitchVelocity();
            currSnapshot.yawVelocity = handler.getYawVelocity();
            currSnapshot.activity = handler.getActivity();
            currSnapshot.recoilHeat = recoilHeat;
            currSnapshot.sinceShot = sinceShot;
            currSnapshot.randomSeed = randomSeed;
            currSnapshot.randomSeed2 = randomSeed2;
            lastPhysicsUpdateNanos = System.nanoTime();
        }
    }

    @Override
    public void onShoot(Player player) {
        onShoot();
    }

    /** Demo / 测试用：不依赖 Player 的开火入口。 */
    public void onShoot() {
        handler.onShoot();
        lastShootTime = time;
        hasShot = true;
        // 旧 applyCamImpulse 里的热度累加：每发 +1/shotsToStable，封顶 1。
        recoilHeat = Math.min(1f, recoilHeat + 1f / data.recoilHeat().shotsToStable());
        // VisualRecoilMix 的相位种子；旧实现是在 applyImpulse 里随每发重置的。
        randomSeed = random.nextFloat();
        randomSeed2 = random.nextFloat();
        // 让渲染线程立刻拿到新的 seed / sinceShot=0 / heat，而不是等下一次 100Hz 推进。
        publishSnapshot();
    }

    @Override
    public void applyImpulse(float impulseZ, float pitch, float yaw,
                             float globalPitch, float globalYaw, float shakeRoll) {
        // Demo：新模型是目标驱动，不再使用旧冲量。
    }

    /**
     * 把两层随机旋转和旧 {@link VisualRecoilMix} 的附加抖动合成到枪模 pose 上。
     *
     * <p>组合顺序沿用旧 {@code RecoilUpdater}：先应用 VisualRecoilMix 的位移抖动并绕握把
     * 枢轴（{@link IGunModel#getHandRotPivot()} 的 z）做 global 旋转，再绕模型原点做 local
     * 旋转并叠加 mix 的旋转抖动，最后应用 mix 的后移 Z。</p>
     *
     * <p>物理线程 100Hz、渲染帧率不固定，所以角度在上一帧和当前帧快照之间线性插值，
     * 避免掉帧时枪模一跳一跳。ADS 因子读取 {@link Client#getAimingProgress()} 和
     * {@link Client#WEAPON_STATUS} 的 z 补偿，和旧实现保持一致。</p>
     */
    @Override
    public void applyTransformPost(PoseStack poseStack, boolean aiming,
                                   float particleTicks, IGunModel model) {
        FrameSample frame = sampleFrame();

        float globalPitch = frame.globalPitch();
        float globalYaw = frame.globalYaw();
        float localPitch = frame.localPitch();
        float localYaw = frame.localYaw();

        float aimingProgress = Client.getAimingProgress();
        aimingProgress *= aimingProgress;
        float adsZCompensation = Client.WEAPON_STATUS.getLerpAdsZCompensation(particleTicks);

        VisualShake shake = computeVisualShake(frame, aimingProgress);
        float zBack = shake.backZ() * Mth.lerp(aimingProgress, 1f, adsZCompensation);

        boolean hasPose = Math.abs(globalPitch) >= POSE_EPSILON_DEG
                || Math.abs(globalYaw) >= POSE_EPSILON_DEG
                || Math.abs(localPitch) >= POSE_EPSILON_DEG
                || Math.abs(localYaw) >= POSE_EPSILON_DEG;
        boolean hasShake = Math.abs(shake.x()) + Math.abs(shake.y())
                + Math.abs(shake.rotX()) + Math.abs(shake.rotY())
                + Math.abs(zBack) >= data.visualShake().epsilon();
        if (!hasPose && !hasShake) {
            return;
        }

        float pivotZ = handRotPivotZ(model);
        poseStack.translate(shake.x(), shake.y() * 0.5f, pivotZ);
        applyPoseRotation(poseStack, globalPitch, globalYaw);
        poseStack.translate(0f, 0f, -pivotZ);
        applyPoseRotation(poseStack, localPitch + shake.rotX(), localYaw + shake.rotY());
        poseStack.translate(0f, 0f, zBack);
    }

    /**
     * 旧 {@code RecoilUpdater.applyTransformPost} 里的 VisualRecoilMix 部分：开火后按 mix
     * 参数生成额外的位移抖动（shakeX/Y）、旋转抖动（shakeRotX/Y）和后移 Z。
     *
     * <p>ADS 的插值沿用旧实现：aimingProgress 先平方，再在“腰射值 1”和 mix 的 ADS 因子之间
     * lerp。demo 没有后移位移，所以 back 层的 omega 固定取 {@code backOmegaMin}
     * （旧实现会按后移位移在 min/max 之间插值）。</p>
     */
    private VisualShake computeVisualShake(FrameSample frame, float aimingProgress) {
        RecoilData recoilData = legacyData;
        VisualRecoilMix mix = recoilData == null ? null : recoilData.getVisualRecoilMix();
        if (mix == null) {
            return VisualShake.ZERO;
        }

        VisualShakeProfile profile = data.visualShake();
        float sinceShot = frame.sinceShot() + frame.ageNanos() / 1_000_000_000f;
        if (!(sinceShot >= 0f) || sinceShot >= profile.timeLimit()) {
            return VisualShake.ZERO;
        }

        float heat = frame.recoilHeat();
        float shakeAds = Mth.lerp(aimingProgress, 1f, mix.shakeAdsScaleFactor());
        float backOmegaAds = Mth.lerp(aimingProgress, 1f, mix.backAdsOmegaFactor());
        float rotOmegaAds = Mth.lerp(aimingProgress, 1f, mix.rotAdsOmegaFactor());
        float backZetaAds = Mth.lerp(aimingProgress, 1f, mix.backAdsZetaFactor());
        float rotZetaAds = Mth.lerp(aimingProgress, 1f, mix.rotAdsZetaFactor());

        float scale = (1f + heat * profile.heatShakeScale()) * shakeAds * mix.shakeScale();
        float omega = (1f + heat * profile.heatOmegaScale()) * profile.baseOmega();
        float rand = (frame.randomSeed() * 0.5f + 0.5f) * heat;
        float halfPI = (float) (Math.PI * (profile.phaseBase() + rand * profile.phaseJitter()));
        float shakeX = (float) Utils.dampedOscillation(
                sinceShot, scale, omega, profile.zetaX(), rand * halfPI * profile.shakeXPhaseScale());
        float shakeY = (float) Utils.dampedOscillation(
                sinceShot, scale, omega * profile.shakeYOmegaScale(), profile.zetaY(), rand * halfPI);

        float rotScale = mix.rotScale();
        float ry = Math.signum(frame.randomSeed() - 0.5f)
                * Math.min(frame.randomSeed() + 0.5f, 1f);
        float rx = (Math.signum(frame.randomSeed2() - 0.5f) + 0.5f - aimingProgress * 0.5f)
                * Math.min(frame.randomSeed2() + 0.6f, 1f);
        float rotOmega = mix.rotOmega() * rotOmegaAds;
        float rotZeta = mix.rotZeta() * rotZetaAds;
        float shakeRotY = (float) Utils.dampedOscillation(
                sinceShot, ry * rotScale, rotOmega, rotZeta, profile.rotPhase());
        float shakeRotX = (float) Utils.dampedOscillation(
                sinceShot, rx * rotScale, rotOmega, rotZeta, profile.rotPhase());

        float backOmega = mix.backOmegaMin() * backOmegaAds;
        float backZeta = mix.backZeta() * backZetaAds;
        float visualBackZ = (float) Utils.dampedOscillation(
                sinceShot, mix.backScale(), backOmega, backZeta, profile.backPhase());

        return new VisualShake(shakeX, shakeY, shakeRotX, shakeRotY, visualBackZ);
    }

    /** VisualRecoilMix 在单帧算出的附加量（模型单位 / 度）。 */
    private record VisualShake(float x, float y, float rotX, float rotY, float backZ) {
        public static final VisualShake ZERO = new VisualShake(0f, 0f, 0f, 0f, 0f);
    }

    /**
     * 取当前帧的物理快照和插值系数；渲染线程只允许通过这里读状态。
     *
     * <p>两个快照都是缓存对象，物理线程在锁内原地覆写；这里同样在锁内把需要的值
     * 拷进复用的 {@link FrameSample}，渲染线程不会持有会变的快照引用。</p>
     */
    private FrameSample sampleFrame() {
        synchronized (stateLock) {
            long ageNanos = System.nanoTime() - lastPhysicsUpdateNanos;
            float alpha = (float) (ageNanos / PHYSICS_TICK_NANOS);
            if (!(alpha > 0f)) {
                alpha = 0f;
            } else if (alpha > 1f) {
                alpha = 1f;
            }
            frameSample.fill(prevSnapshot, currSnapshot, alpha, ageNanos);
            return frameSample;
        }
    }

    /** 握把枢轴在模型 z 轴上的位置；模型缺失时退化为绕原点旋转。 */
    private static float handRotPivotZ(IGunModel model) {
        if (model == null) {
            return 0f;
        }
        Bone pivot = model.getHandRotPivot();
        return pivot == null ? 0f : pivot.z;
    }

    /**
     * 按旧 updater 的符号约定绕 X / Y 旋转：pitch 取负、yaw 取正（单位：度）。
     * 这一版没有 roll，Z 轴固定为 0。
     */
    private static void applyPoseRotation(PoseStack poseStack, float pitchDeg, float yawDeg) {
        poseStack.mulPose(new Quaternionf().rotateXYZ(
                -(float) Math.toRadians(pitchDeg),
                (float) Math.toRadians(yawDeg),
                0f));
    }

    /**
     * 一帧的物理状态快照（角度：度，角速度：度/秒）。
     *
     * <p>物理线程持锁原地覆写，渲染线程在同一个锁里把值拷进 {@link FrameSample}；
     * 不再每个 100Hz 帧 new 一个对象。</p>
     */
    private static final class RecoilSnapshot {
        private float globalPitch;
        private float globalYaw;
        private float localPitch;
        private float localYaw;
        private float globalPitchTarget;
        private float globalYawTarget;
        private float localPitchTarget;
        private float localYawTarget;
        private float pitchVelocity;
        private float yawVelocity;
        private float activity;
        private float recoilHeat;
        private float sinceShot = Float.POSITIVE_INFINITY;
        private float randomSeed;
        private float randomSeed2;

        private void copyFrom(RecoilSnapshot other) {
            this.globalPitch = other.globalPitch;
            this.globalYaw = other.globalYaw;
            this.localPitch = other.localPitch;
            this.localYaw = other.localYaw;
            this.globalPitchTarget = other.globalPitchTarget;
            this.globalYawTarget = other.globalYawTarget;
            this.localPitchTarget = other.localPitchTarget;
            this.localYawTarget = other.localYawTarget;
            this.pitchVelocity = other.pitchVelocity;
            this.yawVelocity = other.yawVelocity;
            this.activity = other.activity;
            this.recoilHeat = other.recoilHeat;
            this.sinceShot = other.sinceShot;
            this.randomSeed = other.randomSeed;
            this.randomSeed2 = other.randomSeed2;
        }
    }

    /**
     * 当前渲染帧的采样：最新逻辑值 + 插值后的渲染角度。
     * 由渲染线程在 {@link #stateLock} 内复写并立即使用，不跨帧保存。
     */
    private static final class FrameSample {
        private float globalPitch;
        private float globalYaw;
        private float localPitch;
        private float localYaw;

        private float logicGlobalPitch;
        private float logicGlobalYaw;
        private float logicLocalPitch;
        private float logicLocalYaw;

        private float globalPitchTarget;
        private float globalYawTarget;
        private float localPitchTarget;
        private float localYawTarget;

        private float pitchVelocity;
        private float yawVelocity;
        private float activity;
        private float recoilHeat;
        private float sinceShot;
        private float randomSeed;
        private float randomSeed2;

        private float alpha;
        private long ageNanos;

        private void fill(RecoilSnapshot from, RecoilSnapshot to, float alpha, long ageNanos) {
            this.logicGlobalPitch = to.globalPitch;
            this.logicGlobalYaw = to.globalYaw;
            this.logicLocalPitch = to.localPitch;
            this.logicLocalYaw = to.localYaw;
            this.globalPitchTarget = to.globalPitchTarget;
            this.globalYawTarget = to.globalYawTarget;
            this.localPitchTarget = to.localPitchTarget;
            this.localYawTarget = to.localYawTarget;
            this.pitchVelocity = to.pitchVelocity;
            this.yawVelocity = to.yawVelocity;
            this.activity = to.activity;
            this.recoilHeat = to.recoilHeat;
            this.sinceShot = to.sinceShot;
            this.randomSeed = to.randomSeed;
            this.randomSeed2 = to.randomSeed2;

            this.alpha = alpha;
            this.ageNanos = ageNanos;
            this.globalPitch = lerp(from.globalPitch, to.globalPitch, alpha);
            this.globalYaw = lerp(from.globalYaw, to.globalYaw, alpha);
            this.localPitch = lerp(from.localPitch, to.localPitch, alpha);
            this.localYaw = lerp(from.localYaw, to.localYaw, alpha);
        }

        private float globalPitch() {
            return globalPitch;
        }

        private float globalYaw() {
            return globalYaw;
        }

        private float localPitch() {
            return localPitch;
        }

        private float localYaw() {
            return localYaw;
        }

        private float logicGlobalPitch() {
            return logicGlobalPitch;
        }

        private float logicGlobalYaw() {
            return logicGlobalYaw;
        }

        private float logicLocalPitch() {
            return logicLocalPitch;
        }

        private float logicLocalYaw() {
            return logicLocalYaw;
        }

        private float globalPitchTarget() {
            return globalPitchTarget;
        }

        private float globalYawTarget() {
            return globalYawTarget;
        }

        private float localPitchTarget() {
            return localPitchTarget;
        }

        private float localYawTarget() {
            return localYawTarget;
        }

        private float pitchVelocity() {
            return pitchVelocity;
        }

        private float yawVelocity() {
            return yawVelocity;
        }

        private float activity() {
            return activity;
        }

        private float recoilHeat() {
            return recoilHeat;
        }

        private float sinceShot() {
            return sinceShot;
        }

        private float randomSeed() {
            return randomSeed;
        }

        private float randomSeed2() {
            return randomSeed2;
        }

        private float alpha() {
            return alpha;
        }

        private long ageNanos() {
            return ageNanos;
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    @Override
    public float getGunKickPitch() {
        return getPitch();
    }

    /**
     * 这一发在玩家 yaw 上的偏移（度），会由 {@code Gun.serverShoot} 直接加到
     * {@code shooter.getYRot()} 上。
     *
     * <p>注意符号：枪模 pose 绕 +Y 轴用正角度旋转，而玩家 yaw 增大在方向上正好相反；
     * 所以视觉上的 {@code +yaw} 对应世界 yaw 的 <b>负</b>方向。这里必须取反，
     * 否则子弹会相对枪口朝反方向偏。</p>
     */
    @Override
    public float getGunKickYaw() {
        return poseYawToPlayerYaw(getYaw());
    }

    /** 枪模 pose 的 yaw（绕 +Y 正转）换算到玩家 yaw 方向（取反）。 */
    private static float poseYawToPlayerYaw(float poseYaw) {
        return -poseYaw;
    }

    /**
     * Demo：没有使用旧 RecoilData 的 impulse / controller；只有
     * {@link VisualRecoilMix} 会在 {@link #applyTransformPost} 里参与附加抖动。
     * 保留引用也给 {@link #getRecoilData()} 返回原对象。
     */
    @Override
    public void setRecoilData(RecoilData data) {
        this.legacyData = data;
    }

    @Override
    public RecoilData getRecoilData() {
        return legacyData;
    }

    /** 持续射击热度（0~1），移植自旧 RecoilUpdater 的 recoilHeat。 */
    @Override
    public float getRecoilHeat() {
        return recoilHeat;
    }

    @Override
    public float getCamShakeZ() {
        return 0f;
    }

    @Override
    public float distFromLastShoot() {
        return hasShot ? (float) (time - lastShootTime) : 0f;
    }

    /**
     * 兼容旧相机层：RecoilCameraHandler 把该值当作“每次 update 的增量”累加，
     * 所以这里返回 velocity * dt，而不是 velocity 本身。
     * 新代码如果要绝对偏移，直接用 {@link #getPitch()} / {@link #getYaw()}。
     */
    static Vector2f v = new Vector2f(0,0);
    @Override
    public Vector2f getCameraSpeed() {
        return v;
    }

    /** 两层叠加后的 pitch（度）。 */
    public float getPitch() {
        return handler.getPitch();
    }

    /** 两层叠加后的 yaw（度）。 */
    public float getYaw() {
        return handler.getYaw();
    }

    public float getPitchVelocity() {
        return handler.getPitchVelocity();
    }

    public float getYawVelocity() {
        return handler.getYawVelocity();
    }

    public float getGlobalPitch() {
        return handler.getGlobalPitch();
    }

    public float getGlobalYaw() {
        return handler.getGlobalYaw();
    }

    public float getLocalPitch() {
        return handler.getLocalPitch();
    }

    public float getLocalYaw() {
        return handler.getLocalYaw();
    }

    public float getActivity() {
        return handler.getActivity();
    }

    public NoiseRecoilData getData() {
        return data;
    }

    /**
     * 调试面板用：多行状态文本（{@code "\n"} 分隔），角度单位是度。
     *
     * <p>{@code logic} 是最近一次 100Hz update 后的弹簧状态，{@code render} 是本帧真正
     * 写进 poseStack 的插值角度；两者最多相差一个物理周期，属于正常现象。
     * 所有数值都来自 {@link #sampleFrame()} 的快照，可以在渲染线程安全调用。</p>
     */
    @Override
    public String getDebugInfo() {
        FrameSample frame = sampleFrame();

        StringBuilder sb = new StringBuilder(384);
        sb.append("RecoilDemo  angles in deg\n");
        sb.append(String.format(Locale.ROOT,
                "logic   G p %+7.3f y %+7.3f  L p %+7.3f y %+7.3f\n",
                frame.logicGlobalPitch(), frame.logicGlobalYaw(),
                frame.logicLocalPitch(), frame.logicLocalYaw()));
        sb.append(String.format(Locale.ROOT,
                "target  G p %+7.3f y %+7.3f  L p %+7.3f y %+7.3f\n",
                frame.globalPitchTarget(), frame.globalYawTarget(),
                frame.localPitchTarget(), frame.localYawTarget()));
        sb.append(String.format(Locale.ROOT,
                "        T p %+7.3f y %+7.3f  v p %+7.2f y %+7.2f\n",
                frame.logicGlobalPitch() + frame.logicLocalPitch(),
                frame.logicGlobalYaw() + frame.logicLocalYaw(),
                frame.pitchVelocity(), frame.yawVelocity()));
        sb.append(String.format(Locale.ROOT,
                "render  G p %+7.3f y %+7.3f  L p %+7.3f y %+7.3f\n",
                frame.globalPitch(), frame.globalYaw(), frame.localPitch(), frame.localYaw()));
        sb.append(String.format(Locale.ROOT,
                "        T p %+7.3f y %+7.3f  alpha %.2f\n",
                frame.globalPitch() + frame.localPitch(),
                frame.globalYaw() + frame.localYaw(),
                frame.alpha()));
        sb.append(String.format(Locale.ROOT,
                "kick    p %+7.3f y %+7.3f\n",
                frame.logicGlobalPitch() + frame.logicLocalPitch(),
                poseYawToPlayerYaw(frame.logicGlobalYaw() + frame.logicLocalYaw())));
        sb.append(String.format(Locale.ROOT,
                "act %.3f  heat %.3f  dt %.1f ms", frame.activity(), frame.recoilHeat(),
                frame.ageNanos() / 1_000_000.0f));
        return sb.toString();
    }
}
