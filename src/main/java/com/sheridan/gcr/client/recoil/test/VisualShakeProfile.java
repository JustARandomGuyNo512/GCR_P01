package com.sheridan.gcr.client.recoil.test;

/**
 * 旧 {@code VisualRecoilMix} 在渲染层的响应参数。
 *
 * <p>这些值原来散落在 {@link DemoRecoilUpdater#applyTransformPost} 里的那一串常量中
 * （相位、基频、阻尼、heat 缩放、时间窗、阈值）。集中成 record 并挂到
 * {@link NoiseRecoilData} 上之后，调手感只需要改数据，不用再改逻辑代码。</p>
 *
 * <p>和 {@code VisualRecoilMix} 的分工：Mix 是每把枪自己的幅度 / omega / zeta / ADS 参数，
 * 这里则是所有枪共用的“这套附加抖动怎么算”的响应参数。默认值就是旧
 * {@code RecoilUpdater} 的原始数值。</p>
 *
 * @param baseOmega        位移抖动的基频（rad/s，旧值 22），再按 activity 放大
 * @param zetaX            shakeX 的阻尼比（旧值 0.26）
 * @param zetaY            shakeY 的阻尼比（旧值 0.28）
 * @param rotPhase         旋转抖动 shakeRotX/Y 的初相位（rad，旧值 -π*0.48）
 * @param backPhase        后移 Z 的初相位（rad，旧值 -π*0.5）
 * @param heatShakeScale   幅度随 activity 的放大系数：{@code 1 + heat * x}（旧值 0.6）
 * @param heatOmegaScale   频率随 activity 的放大系数：{@code 1 + heat * x}（旧值 1.5）
 * @param shakeYOmegaScale shakeY 相对 baseOmega 的倍率（旧值 1.1）
 * @param phaseBase        halfPI 的基准系数：{@code π * (phaseBase + rand * phaseJitter)}（旧值 0.45）
 * @param phaseJitter      halfPI 的随机抖动幅度（旧值 0.1）
 * @param shakeXPhaseScale shakeX 相位相对 halfPI 的倍率（旧值 0.67）
 * @param timeLimit        开火后超过这个时间（秒）就不再计算附加抖动（旧值 3）
 * @param epsilon          小于这个值视为 0，避免空跑矩阵运算（旧值 1e-4）
 */
public record VisualShakeProfile(
        float baseOmega,
        float zetaX,
        float zetaY,
        float rotPhase,
        float backPhase,
        float heatShakeScale,
        float heatOmegaScale,
        float shakeYOmegaScale,
        float phaseBase,
        float phaseJitter,
        float shakeXPhaseScale,
        float timeLimit,
        float epsilon
) {
    public VisualShakeProfile {
        requirePositive("baseOmega", baseOmega);
        requireNonNegative("zetaX", zetaX);
        requireNonNegative("zetaY", zetaY);
        requireFinite("rotPhase", rotPhase);
        requireFinite("backPhase", backPhase);
        requireNonNegative("heatShakeScale", heatShakeScale);
        requireNonNegative("heatOmegaScale", heatOmegaScale);
        requirePositive("shakeYOmegaScale", shakeYOmegaScale);
        requireFinite("phaseBase", phaseBase);
        requireNonNegative("phaseJitter", phaseJitter);
        requireNonNegative("shakeXPhaseScale", shakeXPhaseScale);
        requirePositive("timeLimit", timeLimit);
        requireNonNegative("epsilon", epsilon);
    }

    /** 旧 {@code RecoilUpdater.applyTransformPost} 里 VisualRecoilMix 的原始数值。 */
    public static VisualShakeProfile defaults() {
        return new VisualShakeProfile(
                22f,
                0.26f, 0.28f,
                (float) (-Math.PI * 0.48f),
                (float) (-Math.PI * 0.5f),
                0.6f, 1.5f, 1.1f,
                0.45f, 0.1f, 0.67f,
                3f, 1.0e-4f);
    }

    private static void requirePositive(String name, float value) {
        if (!(value > 0f) || !Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
        }
    }

    private static void requireNonNegative(String name, float value) {
        if (!(value >= 0f) || !Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and >= 0: " + value);
        }
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
