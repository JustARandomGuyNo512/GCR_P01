package com.sheridan.gcr.client.recoil;

import net.minecraft.util.Mth;

public record RecoilImpulse(
        float impulseZ,
        float rotPitch,

        float randomLocalPitch,
        float randomLocalYaw,
        float randomGlobalPitch,
        float randomGlobalYaw,
        float randomStart,

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
