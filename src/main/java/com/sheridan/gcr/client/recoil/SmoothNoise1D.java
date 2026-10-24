package com.sheridan.gcr.client.recoil;

public class SmoothNoise1D {

    private final long seed;

    public SmoothNoise1D(long seed) {
        this.seed = seed;
    }

    public float sample(float t) {
        int x0 = fastFloor(t);
        int x1 = x0 + 1;

        float local = t - x0;

        // 平滑插值
        float smooth = smoothStep(local);

        float v0 = randomValue(x0);
        float v1 = randomValue(x1);

        return lerp(v0, v1, smooth);
    }

    private float randomValue(int x) {
        long n = x * 49632L + seed * 325176L;

        n = (n << 13) ^ n;

        long result = (n * (n * n * 15731L + 789221L) + 1376312589L);

        // 映射到 [-1,1]
        return 1.0f - ((result & 0x7fffffff) / 1073741824.0f);
    }

    private float smoothStep(float t) {
        return t * t * (3f - 2f * t);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private int fastFloor(float x) {
        return x >= 0 ? (int)x : (int)x - 1;
    }
}
