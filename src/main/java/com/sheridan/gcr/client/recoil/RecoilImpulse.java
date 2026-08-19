package com.sheridan.gcr.client.recoil;

import net.minecraft.util.Mth;

public record RecoilImpulse(
        float back,
        float rotPitch,

        float randomLocalPitch,
        float randomLocalYaw,
        float randomGlobalPitch,
        float randomGlobalYaw,
        float randomStart,

        float roll
) {

    public float back() {
        return -back;
    }

    public float randomStart() {
        return Mth.clamp(randomStart, 0.1f, 1.0f);
    }
}
