package com.sheridan.gcr.client.recoil.test;

import java.util.Locale;

/**
 * {@link Spring1D} 的手动测试 / 演示。
 *
 * <p>不依赖 Minecraft，直接跑 {@code main} 即可。三组场景：</p>
 * <ol>
 *   <li>用现有 {@code basePitch} 参数（k=145, c=11.5），确认“目标弹簧的阶跃响应”
 *       和“旧冲量弹簧的自由衰减”是同一个运动；</li>
 *   <li>用“105ms 到 95%”反推的快弹簧，对比解析解和 100Hz 显式欧拉；</li>
 *   <li>连发阶梯 target，观察上限和停火回落。</li>
 * </ol>
 */
public final class Spring1DDemo {
    private static final float DT = 0.01f;
    private static final float SETTLE_BAND = 0.02f;

    private Spring1DDemo() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        selfCheck();
        System.out.println("self-check ok");
        System.out.println();

        legacyParity();
        fastResponse();
        burstStaircase();
    }

    /**
     * 最基本的健康检查：能收敛、不 NaN、冲量有界、旧参数下和显式欧拉足够接近。
     */
    private static void selfCheck() {
        Spring1D step = new Spring1D(145f, 11.5f);
        step.setTarget(1f);
        for (int i = 0; i < 1000; i++) {
            step.update(DT);
        }
        check(Math.abs(step.getPosition() - 1f) < 1.0e-3f, "step should converge to target");
        check(Float.isFinite(step.getPosition()) && Float.isFinite(step.getVelocity()),
                "state must stay finite");

        Spring1D impulse = new Spring1D(145f, 11.5f);
        impulse.addImpulse(1f);
        float peak = 0f;
        for (int i = 0; i < 1000; i++) {
            impulse.update(DT);
            peak = Math.max(peak, Math.abs(impulse.getPosition()));
        }
        check(peak > 0f && peak < 0.1f, "impulse response should be bounded, peak=" + peak);

        Spring1D exact = new Spring1D(145f, 11.5f);
        exact.setTarget(1f);
        Spring1D euler = new Spring1D(145f, 11.5f);
        euler.setTarget(1f);
        LegacySpring legacy = new LegacySpring(145f, 11.5f, 1f);
        float exactVsEuler = 0f;
        float eulerVsLegacy = 0f;
        for (int i = 0; i < 200; i++) {
            exact.update(DT);
            euler.updateEuler(DT);
            legacy.update(DT);
            exactVsEuler = Math.max(exactVsEuler,
                    Math.abs(exact.getPosition() - euler.getPosition()));
            eulerVsLegacy = Math.max(eulerVsLegacy,
                    Math.abs(euler.getPosition() - (1f - legacy.position)));
        }
        check(exactVsEuler < 0.08f,
                "exact vs euler should stay close at k=145, diff=" + exactVsEuler);
        check(eulerVsLegacy < 1.0e-4f,
                "euler path should reproduce the old explicit spring, diff=" + eulerVsLegacy);

        Spring1D overdamped = Spring1D.ofResponse(20f, 2f);
        overdamped.setTarget(1f);
        float maxOverdamped = 0f;
        for (int i = 0; i < 1000; i++) {
            overdamped.update(DT);
            maxOverdamped = Math.max(maxOverdamped, overdamped.getPosition());
        }
        check(maxOverdamped <= 1.0001f,
                "overdamped must not overshoot, max=" + maxOverdamped);
        check(Math.abs(overdamped.getPosition() - 1f) < 1.0e-3f,
                "overdamped should converge to target");

        Spring1D undamped = new Spring1D(100f, 0f);
        undamped.addImpulse(1f);
        float maxAbs = 0f;
        for (int i = 0; i < 1000; i++) {
            undamped.update(DT);
            maxAbs = Math.max(maxAbs, Math.abs(undamped.getPosition()));
        }
        check(maxAbs > 0.09f && maxAbs < 0.11f,
                "undamped impulse peak should be ~1/omega, peak=" + maxAbs);

        Spring1D fine = Spring1D.ofResponse(45f, 1f);
        fine.setTarget(1f);
        Spring1D coarse = Spring1D.ofResponse(45f, 1f);
        coarse.setTarget(1f);
        for (int i = 0; i < 1000; i++) {
            fine.update(0.0001f);
        }
        for (int i = 0; i < 10; i++) {
            coarse.update(0.01f);
        }
        check(Math.abs(fine.getPosition() - coarse.getPosition()) < 1.0e-3f,
                "exact update must be frame-rate independent, diff="
                        + Math.abs(fine.getPosition() - coarse.getPosition()));
    }

    /**
     * 场景 1：旧参数下，目标弹簧的阶跃响应 vs 旧冲量弹簧的自由衰减。
     * 线性系统里两者满足 {@code x_step(t) = 1 - x_free(t)}，所以可以直接叠在一起看。
     */
    private static void legacyParity() {
        final float k = 145f;
        final float c = 11.5f;
        final int steps = 200;

        Spring1D exact = new Spring1D(k, c);
        exact.setTarget(1f);
        Spring1D euler = new Spring1D(k, c);
        euler.setTarget(1f);
        LegacySpring legacy = new LegacySpring(k, c, 1f);

        float[] exactCurve = new float[steps + 1];
        float[] eulerCurve = new float[steps + 1];
        float[] legacyCurve = new float[steps + 1];

        for (int i = 0; i <= steps; i++) {
            exactCurve[i] = exact.getPosition();
            eulerCurve[i] = euler.getPosition();
            legacyCurve[i] = 1f - legacy.position;
            exact.update(DT);
            euler.updateEuler(DT);
            legacy.update(DT);
        }

        System.out.printf("=== 1. legacy params: k=%.1f, c=%.1f (omega=%.2f, zeta=%.3f) ===%n",
                k, c, exact.getOmega(), exact.getZeta());
        System.out.println("step target=1, dt=10ms; legacy column = 1 - old free decay");
        System.out.println("  t(ms)    exact    euler   legacy");
        for (int i = 0; i <= 50; i += 5) {
            System.out.printf("  %5d  %8.4f %8.4f %8.4f%n",
                    i * 10, exactCurve[i], eulerCurve[i], legacyCurve[i]);
        }
        System.out.println();
        printMetrics("exact", exactCurve, 1f);
        printMetrics("euler", eulerCurve, 1f);
        printMetrics("legacy", legacyCurve, 1f);
        System.out.printf("  max |exact - legacy| = %.5f%n", maxDiff(exactCurve, legacyCurve));
        System.out.printf("  max |euler - legacy| = %.5f%n", maxDiff(eulerCurve, legacyCurve));
        System.out.println();
    }

    /**
     * 场景 2：105ms 到 95% 的快弹簧。解析解和 100Hz 显式欧拉对比。
     * 这一组是为了说明“上升时间越短，旧积分器越不够用”。
     */
    private static void fastResponse() {
        final float omega = 45f;
        final float zeta = 1f;
        final int steps = 30;

        Spring1D exact = Spring1D.ofResponse(omega, zeta);
        exact.setTarget(1f);
        Spring1D euler = Spring1D.ofResponse(omega, zeta);
        euler.setTarget(1f);

        float[] exactCurve = new float[steps + 1];
        float[] eulerCurve = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            exactCurve[i] = exact.getPosition();
            eulerCurve[i] = euler.getPosition();
            exact.update(DT);
            euler.updateEuler(DT);
        }

        System.out.printf("=== 2. fast spring: omega=%.0f, zeta=%.0f (t95 ~ %.0f ms) ===%n",
                omega, zeta, 4.74 / omega * 1000f);
        System.out.println("target=1, dt=10ms; explicit Euler already distorts at omega*dt=0.45");
        System.out.println("  t(ms)    exact    euler      diff");
        for (int i = 0; i <= 10; i++) {
            System.out.printf("  %5d  %8.4f %8.4f %9.4f%n",
                    i * 10, exactCurve[i], eulerCurve[i],
                    Math.abs(exactCurve[i] - eulerCurve[i]));
        }
        System.out.println();
        printMetrics("exact", exactCurve, 1f);
        printMetrics("euler", eulerCurve, 1f);
        System.out.printf("  at 10ms: exact=%.4f, euler=%.4f%n", exactCurve[1], eulerCurve[1]);
        System.out.println();
    }

    /**
     * 场景 3：每 80ms 加 0.55，上限 8；停火后 target 回 0，看回落时间。
     */
    private static void burstStaircase() {
        final float omega = 45f;
        final float zeta = 1f;
        final float perShot = 0.55f;
        final float maxPitch = 8f;
        final int shotIntervalSteps = 8;
        final int shots = 15;
        final int steps = shots * shotIntervalSteps + 50;

        Spring1D spring = Spring1D.ofResponse(omega, zeta);
        spring.setTarget(0f);

        System.out.printf("=== 3. burst staircase: +%.2f every 80ms, cap %.2f ===%n",
                perShot, maxPitch);
        System.out.println("  shot   t(ms)   target   position");
        float target = 0f;
        for (int i = 0; i <= steps; i++) {
            if (i % shotIntervalSteps == 0 && i / shotIntervalSteps < shots) {
                target = Math.min(target + perShot, maxPitch);
                spring.setTarget(target);
                System.out.printf("  %4d  %6d  %7.2f  %8.4f%n",
                        i / shotIntervalSteps + 1, i * 10, target, spring.getPosition());
            }
            spring.update(DT);
        }

        float releasePosition = spring.getPosition();
        spring.setTarget(0f);
        int recoverSteps = 0;
        while (!spring.isSettled(0.01f, 0.01f) && recoverSteps < 500) {
            spring.update(DT);
            recoverSteps++;
        }
        System.out.printf("  release: target=0, position %.2f -> %.4f in %.0f ms%n",
                releasePosition, spring.getPosition(), recoverSteps * DT * 1000f);
        System.out.println();
    }

    private static void printMetrics(String label, float[] curve, float target) {
        int peakIndex = 0;
        for (int i = 1; i < curve.length; i++) {
            if (curve[i] > curve[peakIndex]) {
                peakIndex = i;
            }
        }

        int t95Index = -1;
        for (int i = 0; i < curve.length; i++) {
            if (curve[i] >= target * 0.95f) {
                t95Index = i;
                break;
            }
        }
        float t95Ms = t95Index < 0 ? Float.NaN : t95Index * DT * 1000f;

        int settleIndex = curve.length - 1;
        float band = Math.abs(target) * SETTLE_BAND;
        while (settleIndex > 0 && Math.abs(curve[settleIndex] - target) <= band) {
            settleIndex--;
        }
        settleIndex = Math.min(settleIndex + 1, curve.length - 1);

        if (peakIndex == curve.length - 1) {
            System.out.printf("  %-6s monotonic to %.4f at end, t95=%4.0f ms, settle(2%%)=%4.0f ms%n",
                    label, curve[peakIndex], t95Ms, settleIndex * DT * 1000f);
        } else {
            System.out.printf("  %-6s peak=%.4f @ %4.0f ms, t95=%4.0f ms, settle(2%%)=%4.0f ms%n",
                    label, curve[peakIndex], peakIndex * DT * 1000f,
                    t95Ms, settleIndex * DT * 1000f);
        }
    }

    private static float maxDiff(float[] a, float[] b) {
        float max = 0f;
        for (int i = 0; i < a.length; i++) {
            max = Math.max(max, Math.abs(a[i] - b[i]));
        }
        return max;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * 旧 {@code RecoilSpring1D.updateExplicit} 的自由衰减版本，用来做对照。
     */
    private static final class LegacySpring {
        private final float stiffness;
        private final float damping;
        private float position;
        private float velocity;

        private LegacySpring(float stiffness, float damping, float position) {
            this.stiffness = stiffness;
            this.damping = damping;
            this.position = position;
        }

        private void update(float dt) {
            float acceleration = -stiffness * position - damping * velocity;
            velocity += acceleration * dt;
            position += velocity * dt;
        }
    }
}
