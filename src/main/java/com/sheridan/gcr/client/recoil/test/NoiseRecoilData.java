package com.sheridan.gcr.client.recoil.test;

/**
 * Demo 版后坐力数据：不再描述“力 / 弹簧常数”，而是描述两层随机旋转的包络。
 *
 * <p>和旧 {@code RecoilData} 的 {@code RecoilImpulse + RecoilController} 不同，
 * 这里不再用冲量 / 刚度 / 阻尼描述“枪怎么动”；每层只有：</p>
 * <ul>
 *   <li>{@code pitchRange / yawRange}：这层最多能摆到多少度；</li>
 *   <li>{@code noiseHz}：噪声推进速度，决定“变相”多快；</li>
 *   <li>{@code noiseAlpha}：噪声目标的 EMA 平滑系数，1 为原始噪声，越小越黏；</li>
 *   <li>{@code omega / zeta}：弹簧跟随的快慢和阻尼，主要影响过程手感；ζ&lt;1 时会有少量超调。</li>
 * </ul>
 *
 * <p>两层叠加：global 大范围慢漂移，local 小范围快抖动。具体的弹簧实现见
 * {@link NoiseRotationSpring}，demo updater 见 {@link DemoRecoilUpdater}。</p>
 */
public record NoiseRecoilData(
        Layer global,
        Layer local,
        float holdTime,
        float recoverTime
) {
    public NoiseRecoilData {
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
    }

    /** Demo 默认值：global 大而慢，local 小而快。 */
    public static NoiseRecoilData defaults() {
        return new NoiseRecoilData(
                new Layer(4f, 4f, 1f, 0.55f, 10f, 0.9f),
                new Layer(2.5f, 2.5f, 5f, 0.8f, 45f, 0.8f),
                0.12f, 0.35f);
    }

    /**
     * 单层随机旋转参数。
     *
     * @param pitchRange 俯仰方向的最大目标幅度（度）
     * @param yawRange   偏航方向的最大目标幅度（度）
     * @param noiseHz    噪声推进速度（Hz）；越大目标变化越快
     * @param noiseAlpha 噪声目标 EMA 系数（0~1，按 100Hz 归一化）；1 为原始噪声，
     *                   0 表示完全冻结在首次采样值（通常不用）
     * @param omega      弹簧固有频率（rad/s）；越大跟得越快
     * @param zeta       阻尼比；1 为临界阻尼，不超调
     */
    public record Layer(
            float pitchRange,
            float yawRange,
            float noiseHz,
            float noiseAlpha,
            float omega,
            float zeta
    ) {
        public Layer {
            requireRange("pitchRange", pitchRange);
            requireRange("yawRange", yawRange);
            if (!(noiseHz >= 0f) || !Float.isFinite(noiseHz)) {
                throw new IllegalArgumentException("noiseHz must be finite and >= 0: " + noiseHz);
            }
            if (!(noiseAlpha >= 0f) || noiseAlpha > 1f || !Float.isFinite(noiseAlpha)) {
                throw new IllegalArgumentException(
                        "noiseAlpha must be finite and in [0, 1]: " + noiseAlpha);
            }
            if (!(omega > 0f) || !Float.isFinite(omega)) {
                throw new IllegalArgumentException("omega must be finite and > 0: " + omega);
            }
            if (!(zeta >= 0f) || !Float.isFinite(zeta)) {
                throw new IllegalArgumentException("zeta must be finite and >= 0: " + zeta);
            }
        }
    }

    private static void requireRange(String name, float value) {
        if (!(value >= 0f) || !Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and >= 0: " + value);
        }
    }
}
