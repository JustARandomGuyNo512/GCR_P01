package com.sheridan.gcr.client.recoil;

import net.minecraft.util.Mth;

public record RecoilImpulse(
        // 主后坐冲量
        float impulseZ,

        // 力矩来源
        float leverArmY,

        // 随机散布
        float randomPitch,
        float randomYaw,
        float randomStart,

        // 高频震动
        float shakeRoll,
        float shakePitch,
        float shakeYaw,
        float shake
) {

    public float impulseZ() {
        return -impulseZ;
    }

    public float randomStart() {
        return Mth.clamp(randomStart, 0.1f, 1.0f);
    }
}
