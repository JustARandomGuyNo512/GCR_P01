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
 * 所以 activity 归零时弹簧会平滑回到 0。{@code noiseAlpha} 则是对噪声本身再做一层
 * EMA 平滑：1 为原始噪声，越小目标越“黏”、高频细节越少。</p>
 *
 * <p>两个轴都用 {@link Spring1D#updateLimited(float)} 推进：靠近目标时速度会被剩余误差
 * 限制住，因此不会带着旧动量越过很近的 target 再折返（见该方法的能量解释）。</p>
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
    private final float noiseAlpha;

    private float phase;
    private float scale = 1f;
    private float smoothedPitchNoise;
    private float smoothedYawNoise;
    private boolean noiseInitialized;

    /**
     * 等价于 {@code noiseAlpha = 1}（直接使用原始噪声）。
     */
    public NoiseRotationSpring(float pitchRange, float yawRange, float noiseHz,
                               float omega, float zeta, long seed) {
        this(pitchRange, yawRange, noiseHz, 1f, omega, zeta, seed);
    }

    /**
     * @param pitchRange pitch 的最大目标幅度（度）
     * @param yawRange   yaw 的最大目标幅度（度）
     * @param noiseHz    噪声推进速度（Hz）；越大目标变化越快
     * @param noiseAlpha 噪声目标 EMA 系数（0~1，按 100Hz 归一化）；1 为原始噪声，越小越黏
     * @param omega      弹簧固有频率（rad/s）；越大跟得越快
     * @param zeta       阻尼比；1 为临界阻尼，不超调
     * @param seed       噪声种子；pitch / yaw 会从同一个种子派生不同序列
     */
    public NoiseRotationSpring(float pitchRange, float yawRange, float noiseHz, float noiseAlpha,
                               float omega, float zeta, long seed) {
        validateRange("pitchRange", pitchRange);
        validateRange("yawRange", yawRange);
        if (!(noiseHz >= 0f) || !Float.isFinite(noiseHz)) {
            throw new IllegalArgumentException("noiseHz must be finite and >= 0: " + noiseHz);
        }
        if (!(noiseAlpha >= 0f) || noiseAlpha > 1f || !Float.isFinite(noiseAlpha)) {
            throw new IllegalArgumentException("noiseAlpha must be finite and in [0, 1]: " + noiseAlpha);
        }

        this.pitchSpring = Spring1D.ofResponse(omega, zeta);
        this.yawSpring = Spring1D.ofResponse(omega, zeta);
        this.pitchNoise = new SmoothNoise1D(seed);
        this.yawNoise = new SmoothNoise1D(seed ^ 0x9E3779B97F4A7C15L);
        this.pitchRange = pitchRange;
        this.yawRange = yawRange;
        this.noiseHz = noiseHz;
        this.noiseAlpha = noiseAlpha;
    }

    /** 推进噪声相位、更新 target、再让两个一维弹簧走一帧。 */
    public void update(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }

        phase += dt * noiseHz;

        float rawPitch = pitchNoise.sample(phase);
        float rawYaw = yawNoise.sample(phase + YAW_PHASE_OFFSET);

        if (!noiseInitialized) {
            // 第一帧直接贴到噪声值，避免每次 reset 后都从 0 慢慢“爬”上来。
            smoothedPitchNoise = rawPitch;
            smoothedYawNoise = rawYaw;
            noiseInitialized = true;
        } else {
            float alpha = alphaForDt(dt);
            smoothedPitchNoise += (rawPitch - smoothedPitchNoise) * alpha;
            smoothedYawNoise += (rawYaw - smoothedYawNoise) * alpha;
        }

        float pitchTarget = smoothedPitchNoise * pitchRange * scale;
        float yawTarget = smoothedYawNoise * yawRange * scale;

        pitchSpring.setTarget(pitchTarget);
        yawSpring.setTarget(yawTarget);
        pitchSpring.updateLimited(dt);
        yawSpring.updateLimited(dt);
    }

    /**
     * 把 {@code noiseAlpha}（按 100Hz 定义的每帧系数）换算到当前 dt。
     * alpha=1 时直接返回 1；alpha=0 时目标完全不动。
     */
    private float alphaForDt(float dt) {
        if (noiseAlpha >= 1f) {
            return 1f;
        }
        if (!(noiseAlpha > 0f)) {
            return 0f;
        }
        return 1f - (float) Math.pow(1.0 - noiseAlpha, dt * 100.0);
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

    /** 清空相位、噪声平滑状态和弹簧；scale 保持不变。 */
    public void reset() {
        phase = 0f;
        smoothedPitchNoise = 0f;
        smoothedYawNoise = 0f;
        noiseInitialized = false;
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

    public float getNoiseAlpha() {
        return noiseAlpha;
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
