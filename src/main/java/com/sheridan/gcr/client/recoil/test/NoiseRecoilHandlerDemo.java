package com.sheridan.gcr.client.recoil.test;

import java.util.Locale;

/**
 * {@link NoiseRecoilHandler} 的手动测试 / 演示。
 *
 * <p>先让 global / local 两层在 scale=1 下自由跑 20 秒，确认：</p>
 * <ul>
 *   <li>global 的 pitch / yaw 范围都明显大于 local；</li>
 *   <li>local 的方向变化明显比 global 频繁；</li>
 *   <li>local 的最大速度小于 global。</li>
 * </ul>
 *
 * <p>再模拟 12 发、每 100ms 一发的连射和停火回落，确认叠加输出严格等于两层之和，
 * 且 activity 归零后输出回到 0。</p>
 */
public final class NoiseRecoilHandlerDemo {
    private static final float DT = 0.01f;
    private static final float CHARACTERIZE_SECONDS = 20f;
    private static final int SHOTS = 12;
    private static final float SHOT_INTERVAL = 0.1f;
    private static final float RECOVERY_TIME = 1.2f;

    private NoiseRecoilHandlerDemo() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        characterizeLayers();
        System.out.println();
        runBurst();
    }

    /**
     * 两层各自跑 20 秒，统计范围 / RMS / 方向变化 / 最大速度。
     * 这一段不经过 handler 的 activity，直接驱动两个弹簧，避免短窗口带来的偶然性。
     */
    private static void characterizeLayers() {
        NoiseRecoilHandler probe = new NoiseRecoilHandler();
        NoiseRotationSpring global = probe.getGlobalSpring();
        NoiseRotationSpring local = probe.getLocalSpring();
        global.setScale(1f);
        local.setScale(1f);

        SeriesStats globalPitch = new SeriesStats();
        SeriesStats globalYaw = new SeriesStats();
        SeriesStats localPitch = new SeriesStats();
        SeriesStats localYaw = new SeriesStats();
        SeriesStats globalPitchVelocity = new SeriesStats();
        SeriesStats localPitchVelocity = new SeriesStats();

        int steps = Math.round(CHARACTERIZE_SECONDS / DT);
        for (int i = 0; i < steps; i++) {
            global.update(DT);
            local.update(DT);

            globalPitch.add(global.getPitch());
            globalYaw.add(global.getYaw());
            localPitch.add(local.getPitch());
            localYaw.add(local.getYaw());
            globalPitchVelocity.add(global.getPitchVelocity());
            localPitchVelocity.add(local.getPitchVelocity());
        }

        System.out.printf("=== layer characterization: %.0fs free run at scale=1 ===%n",
                CHARACTERIZE_SECONDS);
        printStats("global pitch", globalPitch, globalPitchVelocity);
        printStats("global yaw", globalYaw, null);
        printStats("local  pitch", localPitch, localPitchVelocity);
        printStats("local  yaw", localYaw, null);

        check(globalPitch.range() > localPitch.range() * 2f,
                "global pitch range should be clearly larger than local");
        check(globalYaw.range() > localYaw.range() * 2f,
                "global yaw range should be clearly larger than local");
        check(localPitch.directionChanges > globalPitch.directionChanges * 3,
                "local pitch should change direction much more often than global");
        check(localPitchVelocity.maxAbs < globalPitchVelocity.maxAbs,
                "local speed range should be smaller than global");
    }

    /**
     * 12 发连射 + 停火回落，检查叠加关系和 activity 衰减。
     */
    private static void runBurst() {
        NoiseRecoilHandler handler = new NoiseRecoilHandler();

        // 没开火时应该稳定在 0，而不是自己抖。
        for (int i = 0; i < 50; i++) {
            handler.update(DT);
        }
        check(Math.abs(handler.getPitch()) < 1.0e-5f, "idle pitch should stay at 0");
        check(Math.abs(handler.getYaw()) < 1.0e-5f, "idle yaw should stay at 0");

        SeriesStats globalPitch = new SeriesStats();
        SeriesStats localPitch = new SeriesStats();
        SeriesStats sumPitch = new SeriesStats();
        SeriesStats globalYaw = new SeriesStats();
        SeriesStats localYaw = new SeriesStats();
        SeriesStats sumYaw = new SeriesStats();

        int totalSteps = Math.round((SHOTS * SHOT_INTERVAL + RECOVERY_TIME) / DT);
        int nextShot = 0;
        float sumError = 0f;

        System.out.printf("=== burst: %d shots @ %dms, then recovery ===%n",
                SHOTS, Math.round(SHOT_INTERVAL * 1000f));
        System.out.println("note: global is slow; this short burst only covers part of its drift"
                + " (see the 20s stats above)");
        System.out.println("  t(ms)   act   gPitch  lPitch   sumP   gYaw   lYaw   sumY");

        for (int step = 0; step <= totalSteps; step++) {
            if (nextShot < SHOTS && step >= Math.round(nextShot * SHOT_INTERVAL / DT)) {
                handler.onShoot();
                nextShot++;
            }
            handler.update(DT);

            float gp = handler.getGlobalPitch();
            float lp = handler.getLocalPitch();
            float sp = handler.getPitch();
            float gy = handler.getGlobalYaw();
            float ly = handler.getLocalYaw();
            float sy = handler.getYaw();

            globalPitch.add(gp);
            localPitch.add(lp);
            sumPitch.add(sp);
            globalYaw.add(gy);
            localYaw.add(ly);
            sumYaw.add(sy);
            sumError = Math.max(sumError, Math.abs(sp - (gp + lp)));

            if (step % 10 == 0) {
                System.out.printf("  %5.0f  %5.2f  %7.3f %7.3f %7.3f  %7.3f %7.3f %7.3f%n",
                        step * DT * 1000f, handler.getActivity(), gp, lp, sp, gy, ly, sy);
            }
        }

        System.out.println();
        printStats("global pitch", globalPitch, null);
        printStats("local  pitch", localPitch, null);
        printStats("sum    pitch", sumPitch, null);
        printStats("global yaw", globalYaw, null);
        printStats("local  yaw", localYaw, null);
        printStats("sum    yaw", sumYaw, null);

        System.out.println();
        System.out.printf("  combined = global + local, max error = %.7f%n", sumError);
        System.out.printf("  final activity = %.6f, final pitch = %.6f, final yaw = %.6f%n",
                handler.getActivity(), handler.getPitch(), handler.getYaw());

        check(sumError < 1.0e-5f, "combined output must equal global + local");
        check(handler.getActivity() < 1.0e-3f, "activity should decay to zero");
        check(Math.abs(handler.getPitch()) < 1.0e-3f, "pitch should return to zero");
        check(Math.abs(handler.getYaw()) < 1.0e-3f, "yaw should return to zero");
        System.out.println("  checks ok");
    }

    private static void printStats(String label, SeriesStats value, SeriesStats velocity) {
        System.out.printf("  %-12s range [%6.3f, %6.3f] span %.3f, rms %.3f, dir %d",
                label, value.min, value.max, value.range(), value.rms(), value.directionChanges);
        if (velocity != null) {
            System.out.printf(", max|v| %.3f", velocity.maxAbs);
        }
        System.out.println();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * 一路信号的简单统计：范围、RMS、方向变化次数、最大绝对值。
     */
    private static final class SeriesStats {
        private float min = Float.POSITIVE_INFINITY;
        private float max = Float.NEGATIVE_INFINITY;
        private float maxAbs;
        private double sumSq;
        private int count;
        private int directionChanges;
        private float last;
        private int lastSign;

        private void add(float value) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            maxAbs = Math.max(maxAbs, Math.abs(value));
            sumSq += (double) value * value;

            if (count > 0) {
                float delta = value - last;
                int sign = delta > 0f ? 1 : (delta < 0f ? -1 : 0);
                if (sign != 0) {
                    if (lastSign != 0 && sign != lastSign) {
                        directionChanges++;
                    }
                    lastSign = sign;
                }
            }

            last = value;
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
