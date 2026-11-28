package com.sheridan.gcr.client.recoil.test;

import com.sheridan.gcr.client.recoil.SmoothNoise1D;

import java.util.Arrays;

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
 * 乘在 target 上（handler 用它做开火 / 停火的淡入淡出），所以 activity 归零时弹簧
 * 会平滑回到 0。{@code noiseAlpha} 则是对噪声本身再做一层
 * EMA 平滑：1 为原始噪声，越小目标越“黏”、高频细节越少。</p>
 *
 * <p>初始化时用满 activity 的弹簧位置标定两轴的输出增益。运行时只在角度边界附近
 * 平滑压缩，再乘配置角度。弹簧仍按原响应推进，角度参数线性缩放最终输出。</p>
 */
public final class NoiseRotationSpring {
    /** yaw 的相位偏移，避免 pitch / yaw 同步摆动。 */
    private static final float YAW_PHASE_OFFSET = 37.17f;
    private static final float CALIBRATION_DT = 0.01f;
    private static final int CALIBRATION_WARMUP_STEPS = 4096;
    private static final int CALIBRATION_SAMPLE_STEPS = 16384;
    private static final double CALIBRATION_PERCENTILE = 0.95;
    private static final double COMPRESSION_START = 0.8;

    private final Spring1D pitchSpring;
    private final Spring1D yawSpring;
    private final SmoothNoise1D pitchNoise;
    private final SmoothNoise1D yawNoise;
    private final float pitchRange;
    private final float yawRange;
    private final float noiseHz;
    private final float noiseAlpha;
    private final float pitchReference;
    private final float yawReference;

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
     * @param pitchRange pitch 输出的单轴半幅上界（度）
     * @param yawRange   yaw 输出的单轴半幅上界（度）
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

        // 用同一套噪声、EMA 和弹簧推进标定，结束后从初始状态开始运行。
        float[] pitchSamples = new float[CALIBRATION_SAMPLE_STEPS];
        float[] yawSamples = new float[CALIBRATION_SAMPLE_STEPS];
        for (int i = 0; i < CALIBRATION_WARMUP_STEPS; i++) {
            advance(CALIBRATION_DT);
        }
        for (int i = 0; i < CALIBRATION_SAMPLE_STEPS; i++) {
            advance(CALIBRATION_DT);
            pitchSamples[i] = Math.abs(pitchSpring.getPosition());
            yawSamples[i] = Math.abs(yawSpring.getPosition());
        }
        pitchReference = amplitudeReference(pitchSamples);
        yawReference = amplitudeReference(yawSamples);
        clearSpringState();
    }

    /** 推进噪声相位、更新 target、再让两个一维弹簧走一帧。 */
    public void update(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }
        advance(dt);
    }

    private void advance(float dt) {
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

        pitchSpring.setTarget(smoothedPitchNoise * scale);
        yawSpring.setTarget(smoothedYawNoise * scale);
        pitchSpring.updateLimited(dt);
        yawSpring.updateLimited(dt);
    }

    private static float amplitudeReference(float[] samples) {
        Arrays.sort(samples);
        int index = (int) Math.ceil(samples.length * CALIBRATION_PERCENTILE) - 1;
        float reference = samples[index];
        return reference > 0f && Float.isFinite(reference) ? reference : 1f;
    }

    private static float angle(float position, float reference, float range) {
        double normalized = (double) position / reference;
        double magnitude = Math.abs(normalized);
        if (magnitude > COMPRESSION_START) {
            double width = 1.0 - COMPRESSION_START;
            double excess = (magnitude - COMPRESSION_START) / width;
            magnitude = COMPRESSION_START + width * excess / Math.sqrt(1.0 + excess * excess);
        }
        return (float) Math.copySign(magnitude * range, normalized);
    }

    private static float angularVelocity(Spring1D spring, float reference, float range) {
        double normalized = Math.abs((double) spring.getPosition() / reference);
        double velocity = (double) spring.getVelocity() / reference * range;
        if (normalized > COMPRESSION_START) {
            // 对 angle 的压缩函数求导，使速度和位置使用同一条曲线。
            double excess = (normalized - COMPRESSION_START) / (1.0 - COMPRESSION_START);
            double factor = 1.0 + excess * excess;
            velocity /= factor * Math.sqrt(factor);
        }
        return (float) velocity;
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
     * 0 表示目标回到 0（弹簧会平滑回中），1 是幅度标定时的驱动力。
     */
    public void setScale(float scale) {
        if (!Float.isFinite(scale)) {
            throw new IllegalArgumentException("scale must be finite: " + scale);
        }
        this.scale = Math.max(0f, scale);
    }

    /** 清空相位、噪声平滑状态和弹簧；scale 保持不变。 */
    public void reset() {
        clearSpringState();
    }

    private void clearSpringState() {
        phase = 0f;
        smoothedPitchNoise = 0f;
        smoothedYawNoise = 0f;
        noiseInitialized = false;
        pitchSpring.reset();
        yawSpring.reset();
    }

    public float getPitch() {
        return angle(pitchSpring.getPosition(), pitchReference, pitchRange);
    }

    public float getYaw() {
        return angle(yawSpring.getPosition(), yawReference, yawRange);
    }

    public float getPitchVelocity() {
        return angularVelocity(pitchSpring, pitchReference, pitchRange);
    }

    public float getYawVelocity() {
        return angularVelocity(yawSpring, yawReference, yawRange);
    }

    /** 调试用的弹簧驱动目标，按角度增益换算，可超过输出边界。 */
    public float getPitchTarget() {
        return (float) ((double) pitchSpring.getTarget() / pitchReference * pitchRange);
    }

    /** 调试用的弹簧驱动目标，按角度增益换算，可超过输出边界。 */
    public float getYawTarget() {
        return (float) ((double) yawSpring.getTarget() / yawReference * yawRange);
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
