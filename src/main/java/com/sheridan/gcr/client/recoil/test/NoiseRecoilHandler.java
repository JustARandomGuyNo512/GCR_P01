package com.sheridan.gcr.client.recoil.test;

/**
 * 新的 recoilHandler 原型骨架：目前只包含 yaw / pitch 的随机旋转层。
 *
 * <p>global 与 local 两个 {@link NoiseRotationSpring} 直接叠加：</p>
 * <pre>
 *     pitch = global.pitch + local.pitch
 *     yaw   = global.yaw   + local.yaw
 * </pre>
 *
 * <p>开火时 {@link #onShoot()} 把 activity 拉满；停火超过 holdTime 后 activity
 * 按指数衰减，并乘到两个弹簧的 target 上。因此不射击时枪不会一直抖，而是平滑回到 0。</p>
 *
 * <p>这一版还没有确定性抬枪、后移和 roll；那些是后面的层。当前只负责“随机旋转”这一层，
 * 方便单独调 global / local 的范围、速度和相位变化频率。</p>
 */
public final class NoiseRecoilHandler {
    private static final float DEFAULT_HOLD_TIME = 0.12f;
    private static final float DEFAULT_RECOVER_TIME = 0.35f;
    private static final float ACTIVITY_EPSILON = 1.0e-3f;

    private final NoiseRotationSpring global;
    private final NoiseRotationSpring local;
    private final float holdTime;
    private final float recoverTime;

    private float activity;
    private float sinceShot;

    public NoiseRecoilHandler() {
        this(

                new NoiseRotationSpring(1.5f, 1.25f, 0.6f, 9f, 0.9f, 0x9E3779B97F4A7C15L),

                new NoiseRotationSpring(0.22f, 0.26f, 3.8f, 34f, 0.8f, 0xD1B54A32D192ED03L),
                DEFAULT_HOLD_TIME, DEFAULT_RECOVER_TIME);
    }

    public NoiseRecoilHandler(NoiseRotationSpring global, NoiseRotationSpring local,
                              float holdTime, float recoverTime) {
        if (global == null) {
            throw new IllegalArgumentException("global must not be null");
        }
        if (local == null) {
            throw new IllegalArgumentException("local must not be null");
        }
        if (!(holdTime >= 0f) || !Float.isFinite(holdTime)) {
            throw new IllegalArgumentException("holdTime must be finite and >= 0: " + holdTime);
        }
        if (!(recoverTime > 0f) || !Float.isFinite(recoverTime)) {
            throw new IllegalArgumentException("recoverTime must be finite and > 0: " + recoverTime);
        }

        this.global = global;
        this.local = local;
        this.holdTime = holdTime;
        this.recoverTime = recoverTime;
    }

    /** 每一发开火调用一次：随机层保持满幅，并重新开始计算停火时间。 */
    public void onShoot() {
        this.activity = 1f;
        this.sinceShot = 0f;
    }

    /** 每个逻辑帧调用一次（建议 100Hz）。 */
    public void update(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }

        sinceShot += dt;
        if (sinceShot > holdTime) {
            // recoverTime 定义为衰减到 ~2% 的时间，所以时间常数取 1/4。
            float tau = recoverTime / 4f;
            activity *= (float) Math.exp(-dt / tau);
            if (activity < ACTIVITY_EPSILON) {
                activity = 0f;
            }
        }

        global.setScale(activity);
        local.setScale(activity);
        global.update(dt);
        local.update(dt);
    }

    /** 清空活动状态和两个弹簧；下一次 onShoot 从 0 开始。 */
    public void reset() {
        activity = 0f;
        sinceShot = 0f;
        global.reset();
        local.reset();
        global.setScale(0f);
        local.setScale(0f);
    }

    /** 两层叠加后的 pitch，直接给视角 / 模型用（单位与构造参数一致，通常是度）。 */
    public float getPitch() {
        return global.getPitch() + local.getPitch();
    }

    /** 两层叠加后的 yaw。 */
    public float getYaw() {
        return global.getYaw() + local.getYaw();
    }

    public float getPitchVelocity() {
        return global.getPitchVelocity() + local.getPitchVelocity();
    }

    public float getYawVelocity() {
        return global.getYawVelocity() + local.getYawVelocity();
    }

    public float getGlobalPitch() {
        return global.getPitch();
    }

    public float getGlobalYaw() {
        return global.getYaw();
    }

    public float getLocalPitch() {
        return local.getPitch();
    }

    public float getLocalYaw() {
        return local.getYaw();
    }

    public float getGlobalPitchVelocity() {
        return global.getPitchVelocity();
    }

    public float getGlobalYawVelocity() {
        return global.getYawVelocity();
    }

    public float getLocalPitchVelocity() {
        return local.getPitchVelocity();
    }

    public float getLocalYawVelocity() {
        return local.getYawVelocity();
    }

    public float getActivity() {
        return activity;
    }

    /** 调参 / 调试用：直接访问 global 层。 */
    public NoiseRotationSpring getGlobalSpring() {
        return global;
    }

    /** 调参 / 调试用：直接访问 local 层。 */
    public NoiseRotationSpring getLocalSpring() {
        return local;
    }

    public float getHoldTime() {
        return holdTime;
    }

    public float getRecoverTime() {
        return recoverTime;
    }
}
