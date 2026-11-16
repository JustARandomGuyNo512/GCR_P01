package com.sheridan.gcr.client.recoil.test;

import com.sheridan.gcr.client.recoil.SmoothNoise1D;

/**
 * 二维噪声旋转弹簧：pitch / yaw 各一个 {@link Spring1D}，目标由 {@link SmoothNoise1D} 连续采样。
 *
 * <p>同一个类通过参数区分 global / local 两层：</p>
 * <ul>
 *   <li><b>global</b>：range 大、noiseHz 低、omega 小 → 慢速大范围漂移；</li>
 *   <li><b>local</b>：range 小、noiseHz 高、omega 大 → 快速小范围抖动。</li>
 * </ul>
 *
 * <p>噪声每帧采样一次，弹簧负责把“随机点”变成有惯性的运动。{@link #setScale(float)}
 * 乘在 target 上（handler 用它做开火 / 停火的淡入淡出），不会直接改输出，
 * 所以 activity 归零时弹簧会平滑回到 0。</p>
 */
public final class NoiseRotationSpring {
    /** yaw 的相位偏移，避免 pitch / yaw 同步摆动。 */
    private static final float YAW_PHASE_OFFSET = 37.17f;

    private final Spring1D pitchSpring;
    private final Spring1D yawSpring;
    private final SmoothNoise1D pitchNoise;
    private final SmoothNoise1D yawNoise;
    private final float pitchRange;
    private final float yawRange;
    private final float noiseHz;

    private float phase;
    private float scale = 1f;

    /**
     * @param pitchRange pitch 的最大目标幅度（度）
     * @param yawRange   yaw 的最大目标幅度（度）
     * @param noiseHz    噪声推进速度（Hz）；越大目标变化越快
     * @param omega      弹簧固有频率（rad/s）；越大跟得越快
     * @param zeta       阻尼比；1 为临界阻尼，不超调
     * @param seed       噪声种子；pitch / yaw 会从同一个种子派生不同序列
     */
    public NoiseRotationSpring(float pitchRange, float yawRange, float noiseHz,
                               float omega, float zeta, long seed) {
        validateRange("pitchRange", pitchRange);
        validateRange("yawRange", yawRange);
        if (!(noiseHz >= 0f) || !Float.isFinite(noiseHz)) {
            throw new IllegalArgumentException("noiseHz must be finite and >= 0: " + noiseHz);
        }

        this.pitchSpring = Spring1D.ofResponse(omega, zeta);
        this.yawSpring = Spring1D.ofResponse(omega, zeta);
        this.pitchNoise = new SmoothNoise1D(seed);
        this.yawNoise = new SmoothNoise1D(seed ^ 0x9E3779B97F4A7C15L);
        this.pitchRange = pitchRange;
        this.yawRange = yawRange;
        this.noiseHz = noiseHz;
    }

    /** 推进噪声相位、更新 target、再让两个一维弹簧走一帧。 */
    public void update(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }

        phase += dt * noiseHz;

        float pitchTarget = pitchNoise.sample(phase) * pitchRange * scale;
        float yawTarget = yawNoise.sample(phase + YAW_PHASE_OFFSET) * yawRange * scale;

        pitchSpring.setTarget(pitchTarget);
        yawSpring.setTarget(yawTarget);
        pitchSpring.update(dt);
        yawSpring.update(dt);
    }

    /**
     * 目标幅度缩放，通常由 handler 的 activity 驱动。
     * 0 表示目标回到 0（弹簧会平滑回中），1 表示完整幅度。
     */
    public void setScale(float scale) {
        if (!Float.isFinite(scale)) {
            throw new IllegalArgumentException("scale must be finite: " + scale);
        }
        this.scale = Math.max(0f, scale);
    }

    /** 清空相位和弹簧状态；scale 保持不变。 */
    public void reset() {
        phase = 0f;
        pitchSpring.reset();
        yawSpring.reset();
    }

    public float getPitch() {
        return pitchSpring.getPosition();
    }

    public float getYaw() {
        return yawSpring.getPosition();
    }

    public float getPitchVelocity() {
        return pitchSpring.getVelocity();
    }

    public float getYawVelocity() {
        return yawSpring.getVelocity();
    }

    public float getPitchTarget() {
        return pitchSpring.getTarget();
    }

    public float getYawTarget() {
        return yawSpring.getTarget();
    }

    public float getPitchRange() {
        return pitchRange;
    }

    public float getYawRange() {
        return yawRange;
    }

    public float getNoiseHz() {
        return noiseHz;
    }

    public float getScale() {
        return scale;
    }

    private static void validateRange(String name, float value) {
        if (!(value >= 0f) || !Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and >= 0: " + value);
        }
    }
}
