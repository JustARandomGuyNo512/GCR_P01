package com.sheridan.gcr.client.recoil.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.modular.IGunModel;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector2f;

import java.util.Locale;

/**
 * Demo 级 RecoilUpdater：由 {@link NoiseRecoilData} 的随机参数驱动两层噪声弹簧。
 *
 * <p>核心随机层委托给 {@link NoiseRecoilHandler}，这个类只做 {@link IRecoilUpdater}
 * 的接口适配和绝对时间记录。开发环境里可以临时把 {@code RecoilHandler} 里的
 * {@code new RecoilUpdater()} 换成 {@code new DemoRecoilUpdater()} 做手感对比。
 * {@link #setRecoilData(RecoilData)} 只保存引用，不读取任何 impulse / controller / visualMix
 * 参数；实际运动完全由构造时传入的 {@link NoiseRecoilData} 决定。</p>
 *
 * <p>这一版只做随机旋转层：没有确定性抬枪、后移、roll，也没有位移。两层的旋转都会加到
 * {@link #applyTransformPost} 的 poseStack 上：global 层绕 {@link IGunModel#getHandRotPivot()}
 * 转，表现为整把枪被手腕带着慢慢摆；local 层绕模型原点转，表现为枪口附近的高频抖动。
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

    /** 旧 RecoilData：只保留引用给 getRecoilData()，运动完全不用它。 */
    private RecoilData legacyData;

    // --- 渲染线程读取的状态快照 ---
    private final Object stateLock = new Object();
    private RecoilSnapshot prevSnapshot = RecoilSnapshot.ZERO;
    private RecoilSnapshot currSnapshot = RecoilSnapshot.ZERO;
    private volatile long lastPhysicsUpdateNanos = System.nanoTime();

    private double time;
    private double lastShootTime;
    private boolean hasShot;

    public DemoRecoilUpdater() {
        this(defaults());
    }

    public static NoiseRecoilData defaults() {
        return new NoiseRecoilData(
                new NoiseRecoilData.Layer(4f, 4f, 1f, 0.6f, 10f, 0.9f),
                new NoiseRecoilData.Layer(1f, 1f, 12.5f, 0.8f, 35f, 0.7f),
                0.12f, 0.35f);
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

        // 兼容旧 RecoilCameraHandler：它把 getCameraSpeed() 当“每次 update 的增量”累加，
        // 所以这里返回 velocity * dt，而不是 velocity 本身。
        cameraSpeed.set(handler.getPitchVelocity() * dt, handler.getYawVelocity() * dt);

        publishSnapshot();
    }

    /**
     * 把这一帧的两层弹簧状态打包成不可变快照，发布给渲染线程。
     *
     * <p>除了渲染插值要用的角度，还带上角速度和 activity，让调试面板可以在渲染线程上
     * 安全地显示“逻辑数值”，不用跨线程直接读 handler。</p>
     */
    private void publishSnapshot() {
        RecoilSnapshot sample = new RecoilSnapshot(
                handler.getGlobalPitch(), handler.getGlobalYaw(),
                handler.getLocalPitch(), handler.getLocalYaw(),
                handler.getPitchVelocity(), handler.getYawVelocity(),
                handler.getActivity());
        synchronized (stateLock) {
            prevSnapshot = currSnapshot;
            currSnapshot = sample;
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
    }

    @Override
    public void applyImpulse(float impulseZ, float pitch, float yaw,
                             float globalPitch, float globalYaw, float shakeRoll) {
        // Demo：新模型是目标驱动，不再使用旧冲量。
    }

    /**
     * 把两层随机旋转合成到枪模 pose 上。
     *
     * <p>组合顺序沿用旧 {@code RecoilUpdater}：先让 global 层绕握把枢轴
     * （{@link IGunModel#getHandRotPivot()} 的 z）旋转，整把枪像被手腕带着摆；再让
     * local 层绕模型原点旋转，得到枪口附近的高频抖动。这一版没有位移、roll 和
     * ADS 补偿，所以除了绕枢轴的那次平移，不会在 z 上做任何回退。</p>
     *
     * <p>物理线程 100Hz、渲染帧率不固定，所以角度在上一帧和当前帧快照之间线性插值，
     * 避免掉帧时枪模一跳一跳。{@code aiming} / {@code particleTicks} 暂时不用：新的
     * 数据模型里还没有 ADS 缩放参数，加在这里会变成写死的调参。</p>
     */
    @Override
    public void applyTransformPost(PoseStack poseStack, boolean aiming,
                                   float particleTicks, IGunModel model) {
        FrameSample frame = sampleFrame();
        float globalPitch = frame.globalPitch();
        float globalYaw = frame.globalYaw();
        float localPitch = frame.localPitch();
        float localYaw = frame.localYaw();

        if (Math.abs(globalPitch) < POSE_EPSILON_DEG
                && Math.abs(globalYaw) < POSE_EPSILON_DEG
                && Math.abs(localPitch) < POSE_EPSILON_DEG
                && Math.abs(localYaw) < POSE_EPSILON_DEG) {
            return;
        }

        float pivotZ = handRotPivotZ(model);
        if (globalPitch != 0f || globalYaw != 0f) {
            poseStack.translate(0f, 0f, pivotZ);
            applyPoseRotation(poseStack, globalPitch, globalYaw);
            poseStack.translate(0f, 0f, -pivotZ);
        }
        if (localPitch != 0f || localYaw != 0f) {
            applyPoseRotation(poseStack, localPitch, localYaw);
        }
    }

    /**
     * 取当前帧的物理快照和插值系数；渲染线程只允许通过这里读状态。
     */
    private FrameSample sampleFrame() {
        RecoilSnapshot from;
        RecoilSnapshot to;
        long stamp;
        synchronized (stateLock) {
            from = prevSnapshot;
            to = currSnapshot;
            stamp = lastPhysicsUpdateNanos;
        }

        long ageNanos = System.nanoTime() - stamp;
        float alpha = (float) (ageNanos / PHYSICS_TICK_NANOS);
        if (!(alpha > 0f)) {
            alpha = 0f;
        } else if (alpha > 1f) {
            alpha = 1f;
        }
        return new FrameSample(from, to, alpha, ageNanos);
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

    /** 一帧的物理状态快照（角度：度，角速度：度/秒）。物理线程写、渲染线程读。 */
    private record RecoilSnapshot(float globalPitch, float globalYaw,
                                  float localPitch, float localYaw,
                                  float pitchVelocity, float yawVelocity,
                                  float activity) {
        private static final RecoilSnapshot ZERO =
                new RecoilSnapshot(0f, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    /**
     * 当前渲染帧：上一帧/当前帧快照 + 插值系数。角度由方法现算，不额外存字段。
     */
    private record FrameSample(RecoilSnapshot from, RecoilSnapshot to, float alpha, long ageNanos) {
        private float globalPitch() {
            return lerp(from.globalPitch(), to.globalPitch(), alpha);
        }

        private float globalYaw() {
            return lerp(from.globalYaw(), to.globalYaw(), alpha);
        }

        private float localPitch() {
            return lerp(from.localPitch(), to.localPitch(), alpha);
        }

        private float localYaw() {
            return lerp(from.localYaw(), to.localYaw(), alpha);
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
     * Demo：完全忽略旧 RecoilData 的 impulse / controller / visualMix。
     * 保留引用只是为了 {@link #getRecoilData()} 能返回原对象；运动只由
     * {@link NoiseRecoilData} 决定。
     */
    @Override
    public void setRecoilData(RecoilData data) {
        this.legacyData = data;
    }

    @Override
    public RecoilData getRecoilData() {
        return legacyData;
    }

    /** Demo 用 activity 代替旧的 heat，范围同样是 0~1。 */
    @Override
    public float getRecoilHeat() {
        return handler.getActivity();
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
        RecoilSnapshot logic = frame.to();

        StringBuilder sb = new StringBuilder(384);
        sb.append("RecoilDemo  angles in deg\n");
        sb.append(String.format(Locale.ROOT,
                "logic   G p %+7.3f y %+7.3f  L p %+7.3f y %+7.3f\n",
                logic.globalPitch(), logic.globalYaw(), logic.localPitch(), logic.localYaw()));
        sb.append(String.format(Locale.ROOT,
                "        T p %+7.3f y %+7.3f  v p %+7.2f y %+7.2f\n",
                logic.globalPitch() + logic.localPitch(),
                logic.globalYaw() + logic.localYaw(),
                logic.pitchVelocity(), logic.yawVelocity()));
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
                logic.globalPitch() + logic.localPitch(),
                poseYawToPlayerYaw(logic.globalYaw() + logic.localYaw())));
        sb.append(String.format(Locale.ROOT,
                "act %.3f  dt %.1f ms", logic.activity(),
                frame.ageNanos() / 1_000_000.0f));
        return sb.toString();
    }
}
