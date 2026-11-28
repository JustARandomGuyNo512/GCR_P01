package com.sheridan.gcr.client.recoil.test;

import com.sheridan.gcr.client.recoil.RecoilController;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.client.recoil.RecoilImpulse;
import com.sheridan.gcr.client.recoil.VisualRecoilMix;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Locale;

/**
 * {@link DemoRecoilUpdater} 的手动测试 / 演示。
 *
 * <p>验证三件事：</p>
 * <ol>
 *   <li>给 {@code setRecoilData(...)} 传一份极端离谱的旧 RecoilData，运动完全不变；</li>
 *   <li>换一份 {@link NoiseRecoilData}（只改 global 范围），运动确实改变；</li>
 *   <li>12 发连射后停火，activity 和输出都能回到 0。</li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
public final class DemoRecoilUpdaterMain {
    private static final float DT = 0.01f;
    private static final int SHOTS = 12;
    private static final float SHOT_INTERVAL = 0.1f;
    private static final float RECOVERY = 1.0f;

    private DemoRecoilUpdaterMain() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);

        DemoRecoilUpdater plain = new DemoRecoilUpdater();
        DemoRecoilUpdater withLegacy = new DemoRecoilUpdater();
        RecoilData extremeLegacy = extremeLegacyData();
        withLegacy.setRecoilData(extremeLegacy);

        float legacyDiff = runPair(plain, withLegacy);
        System.out.printf("legacy RecoilData ignored: max diff = %.7f%n", legacyDiff);
        check(legacyDiff < 1.0e-6f, "legacy RecoilData must not affect motion");
        check(withLegacy.getRecoilData() == extremeLegacy,
                "legacy reference should be kept for getRecoilData()");

        NoiseRecoilData wide = new NoiseRecoilData(
                new NoiseRecoilData.Layer(3.0f, 2.5f, 0.6f, 0.55f, 9f, 0.9f),
                NoiseRecoilData.defaults().local(),
                0.12f, 0.35f,
                VisualShakeProfile.defaults(),
                RecoilHeatProfile.defaults());
        DemoRecoilUpdater custom = new DemoRecoilUpdater(wide);
        DemoRecoilUpdater baseline = new DemoRecoilUpdater();
        float dataDiff = runPair(custom, baseline);
        System.out.printf("custom NoiseRecoilData changes motion: max diff = %.4f%n", dataDiff);
        check(dataDiff > 0.1f, "NoiseRecoilData should actually drive the motion");

        checkNoiseAlpha();

        System.out.println();
        runAndPrint(new DemoRecoilUpdater());
    }

    /**
     * noiseAlpha 的直接验证：同一噪声、同一弹簧，alpha 越低，目标越黏，总变差应该更小。
     */
    private static void checkNoiseAlpha() {
        NoiseRotationSpring raw = new NoiseRotationSpring(
                1f, 1f, 4f, 1f, 30f, 0.8f, 12345L);
        NoiseRotationSpring smooth = new NoiseRotationSpring(
                1f, 1f, 4f, 0.1f, 30f, 0.8f, 12345L);

        float rawVariation = totalVariation(raw);
        float smoothVariation = totalVariation(smooth);
        System.out.printf("noiseAlpha: raw variation=%.3f, smooth variation=%.3f%n",
                rawVariation, smoothVariation);
        check(smoothVariation < rawVariation,
                "lower noiseAlpha should reduce target variation");
    }

    private static float totalVariation(NoiseRotationSpring spring) {
        float total = 0f;
        float last = spring.getPitchTarget();

        for (int i = 0; i < 400; i++) {
            spring.update(DT);
            float value = spring.getPitchTarget();
            total += Math.abs(value - last);
            last = value;
        }
        return total;
    }

    private static float runPair(DemoRecoilUpdater a, DemoRecoilUpdater b) {
        int totalSteps = Math.round((SHOTS * SHOT_INTERVAL + RECOVERY) / DT);
        int nextShot = 0;
        float maxDiff = 0f;

        for (int step = 0; step <= totalSteps; step++) {
            if (nextShot < SHOTS && step >= Math.round(nextShot * SHOT_INTERVAL / DT)) {
                a.onShoot();
                b.onShoot();
                nextShot++;
            }
            a.update(DT);
            b.update(DT);
            maxDiff = Math.max(maxDiff, Math.abs(a.getPitch() - b.getPitch()));
            maxDiff = Math.max(maxDiff, Math.abs(a.getYaw() - b.getYaw()));
        }
        return maxDiff;
    }

    private static void runAndPrint(DemoRecoilUpdater updater) {
        int totalSteps = Math.round((SHOTS * SHOT_INTERVAL + RECOVERY) / DT);
        int nextShot = 0;
        SeriesStats pitch = new SeriesStats();
        SeriesStats yaw = new SeriesStats();

        System.out.printf("=== DemoRecoilUpdater: %d shots @ %dms, then %.1fs recovery ===%n",
                SHOTS, Math.round(SHOT_INTERVAL * 1000f), RECOVERY);
        System.out.println("  t(ms)   act   pitch     yaw");

        for (int step = 0; step <= totalSteps; step++) {
            if (nextShot < SHOTS && step >= Math.round(nextShot * SHOT_INTERVAL / DT)) {
                updater.onShoot();
                nextShot++;
            }
            updater.update(DT);
            pitch.add(updater.getPitch());
            yaw.add(updater.getYaw());

            if (step % 10 == 0) {
                System.out.printf("  %5.0f  %5.2f  %7.3f  %7.3f%n",
                        step * DT * 1000f, updater.getActivity(),
                        updater.getPitch(), updater.getYaw());
            }
        }

        System.out.println();
        System.out.printf("  pitch range [%6.3f, %6.3f] span %.3f, rms %.3f%n",
                pitch.min, pitch.max, pitch.range(), pitch.rms());
        System.out.printf("  yaw   range [%6.3f, %6.3f] span %.3f, rms %.3f%n",
                yaw.min, yaw.max, yaw.range(), yaw.rms());
        System.out.printf("  final activity = %.6f, pitch = %.6f, yaw = %.6f%n",
                updater.getActivity(), updater.getPitch(), updater.getYaw());

        check(updater.getActivity() < 1.0e-3f, "activity should decay to zero");
        check(Math.abs(updater.getPitch()) < 1.0e-3f, "pitch should return to zero");
        check(Math.abs(updater.getYaw()) < 1.0e-3f, "yaw should return to zero");
        System.out.println("  checks ok");
    }

    /**
     * 故意构造一份极端旧参数；DemoRecoilUpdater 不应该读取其中任何一个字段。
     */
    private static RecoilData extremeLegacyData() {
        return new RecoilData(
                new RecoilImpulse(
                        999f, 999f, 999f, 999f, 999f, 999f, 1f, 1f, 999f),
                new RecoilController(
                        999f, 999f, 999f, 999f, 999f,
                        999f, 999f, 999f, 999f, 999f,
                        999f, 999f, 999f, 999f, 999f),
                new VisualRecoilMix(
                        999f, 999f, 999f, 999f, 999f, 999f,
                        999f, 999f, 999f, 999f, 999f,
                        999f, 999f));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class SeriesStats {
        private float min = Float.POSITIVE_INFINITY;
        private float max = Float.NEGATIVE_INFINITY;
        private double sumSq;
        private int count;

        private void add(float value) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sumSq += (double) value * value;
            count++;
        }

        private float range() {
            return max - min;
        }

        private float rms() {
            return (float) Math.sqrt(sumSq / count);
        }
    }
}
