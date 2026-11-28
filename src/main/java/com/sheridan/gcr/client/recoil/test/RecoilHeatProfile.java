package com.sheridan.gcr.client.recoil.test;

/**
 * 持续射击时的后坐力热度（recoilHeat）参数，移植自旧 {@code RecoilUpdater}。
 *
 * <p>heat 在每一发开火时按“稳定所需发数”的倒数累加，停火超过
 * {@code 武器射击间隔 + recoveryDelay} 后按 {@code recoveryRate} 逐帧回落。
 * 它本身只描述“连续射击把枪压到了多热”，不直接改任何输出；要和视觉/镜头联动时
 * 再从 updater 读取。</p>
 *
 * @param stableDuration   旧 {@code RecoilController.stableDuration}：控制为 1 时打满 heat 所需发数
 * @param recoilControl    旧 {@code WeaponStatus.getRecoilControl()}：越大越快打满 heat、回落也越快
 * @param baseRecoveryRate 每帧基础回落量（旧值 0.082，按 100Hz 调参）
 * @param recoveryDelay    停火判定在武器射击间隔之上额外等待的时间（秒，旧值 0.05）
 * @param minShotsToStable 打满 heat 的最少发数下限（旧值 1.5）
 * @param maxShotsToStable 打满 heat 的最多发数上限（旧值 25）
 * @param minRecoveryRate  每帧最低回落量，避免高控制时回落太慢/为 0（旧值 0.1）
 */
public record RecoilHeatProfile(
        float stableDuration,
        float recoilControl,
        float baseRecoveryRate,
        float recoveryDelay,
        float minShotsToStable,
        float maxShotsToStable,
        float minRecoveryRate
) {
    public RecoilHeatProfile {
        requirePositive("stableDuration", stableDuration);
        requirePositive("recoilControl", recoilControl);
        requireNonNegative("baseRecoveryRate", baseRecoveryRate);
        requireNonNegative("recoveryDelay", recoveryDelay);
        requirePositive("minShotsToStable", minShotsToStable);
        requirePositive("maxShotsToStable", maxShotsToStable);
        requireNonNegative("minRecoveryRate", minRecoveryRate);
        if (maxShotsToStable < minShotsToStable) {
            throw new IllegalArgumentException(
                    "maxShotsToStable must be >= minShotsToStable: "
                            + maxShotsToStable + " < " + minShotsToStable);
        }
    }

    /** 旧 RecoilUpdater / M4A1 一类的默认值。 */
    public static RecoilHeatProfile defaults() {
        return new RecoilHeatProfile(13f, 1f, 0.082f, 0.05f, 1.5f, 25f, 0.1f);
    }

    /**
     * 旧公式：{@code clamp(stableDuration / recoilControl, min, max)}。
     * 打满 heat 需要的发数。
     */
    public float shotsToStable() {
        float shots = stableDuration / recoilControl;
        return Math.max(minShotsToStable, Math.min(maxShotsToStable, shots));
    }

    /**
     * 旧公式：{@code max(minRecoveryRate, baseRecoveryRate * recoilControl)}。
     * 每个 update 回落的热量。
     */
    public float recoveryRate() {
        return Math.max(minRecoveryRate, baseRecoveryRate * recoilControl);
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
}
