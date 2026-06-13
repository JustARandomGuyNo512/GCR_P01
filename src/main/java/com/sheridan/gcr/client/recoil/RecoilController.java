package com.sheridan.gcr.client.recoil;

public record RecoilController(
        // 线性恢复
        float linearZStiffness,
        float linearZDamping,

        // 角恢复
        float pitchStiffness,
        float pitchDamping,

        float randomStiffnessPitch,
        float randomDampingPitch,
        float randomStiffnessYaw,
        float randomDampingYaw,

        float rollStiffness,
        float rollDamping,

        // ADS 修正
        float motionAdsModifierStiff,
        float motionAdsModifierDamp,

        float rotAdsModifierStiff,
        float rotAdsModifierDamp,

        float stableDuration
) {
}