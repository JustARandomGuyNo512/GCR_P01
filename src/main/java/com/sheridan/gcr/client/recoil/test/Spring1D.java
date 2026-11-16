package com.sheridan.gcr.client.recoil.test;

import java.util.Locale;

/**
 * 一维目标弹簧（后坐力参数化原型）。
 *
 * <p>运动方程（质量归一化为 1）：</p>
 * <pre>
 *     x'' = k * (target - x) - c * x'
 * </pre>
 *
 * <p>和 {@code RecoilUpdater.RecoilSpring1D} 的区别只有一个：旧弹簧的静止位置固定为 0，
 * 靠 {@code addVelocity} 打冲量起振；这里把静止位置显式成 {@link #setTarget(float)}，
 * 于是可以直接写“这一发要抬到 3 度”，弹簧负责把枪 / 视角带过去。</p>
 *
 * <p>{@link #update(float)} 用解析解推进（target 在单帧内视为常量），结果与帧率无关；
 * {@link #updateEuler(float)} 和 {@link #updateImplicit(float)} 保留旧的积分方式，
 * 只用于和现有手感做 A/B，不建议在新逻辑里使用。</p>
 *
 * <p>单位：{@code position / target} 可以是角度、模型单位或像素；
 * {@code stiffness} 的单位是 1/s²，{@code damping} 的单位是 1/s。
 * 需要按“多久到、超调多少”调参时，用 {@link #ofResponse(float, float)}。</p>
 */
public final class Spring1D {
    private float target;
    private float position;
    private float velocity;
    private float stiffness;
    private float damping;

    public Spring1D(float stiffness, float damping) {
        setStiffness(stiffness);
        setDamping(damping);
    }

    /**
     * 按“固有频率 + 阻尼比”构造。
     *
     * @param omega 固有角频率（rad/s），越大越快
     * @param zeta  阻尼比，1 为临界阻尼（无超调）
     */
    public static Spring1D ofResponse(float omega, float zeta) {
        validateResponse(omega, zeta);
        return new Spring1D(omega * omega, 2f * zeta * omega);
    }

    public void setStiffness(float stiffness) {
        if (!(stiffness > 0f) || !Float.isFinite(stiffness)) {
            throw new IllegalArgumentException("stiffness must be finite and > 0: " + stiffness);
        }
        this.stiffness = stiffness;
    }

    public void setDamping(float damping) {
        if (!(damping >= 0f) || !Float.isFinite(damping)) {
            throw new IllegalArgumentException("damping must be finite and >= 0: " + damping);
        }
        this.damping = damping;
    }

    /** 直接按 ω / ζ 改参数，内部换算成 k = ω²、c = 2ζω。 */
    public void setResponse(float omega, float zeta) {
        validateResponse(omega, zeta);
        setStiffness(omega * omega);
        setDamping(2f * zeta * omega);
    }

    /** 设置弹簧的静止位置。只改目标，不会重置速度和当前位置。 */
    public void setTarget(float target) {
        if (!Float.isFinite(target)) {
            throw new IllegalArgumentException("target must be finite: " + target);
        }
        this.target = target;
    }

    /** 在当前 target 上叠加一个增量。 */
    public void addTarget(float delta) {
        if (!Float.isFinite(delta)) {
            throw new IllegalArgumentException("delta must be finite: " + delta);
        }
        setTarget(target + delta);
    }

    /**
     * 给弹簧一个速度冲量，效果等同于旧 {@code RecoilSpring1D.addVelocity}。
     * 冲量和 target 是叠加的：可以用来在目标运动之上再加一次“枪口跳动”。
     */
    public void addImpulse(float deltaVelocity) {
        if (!Float.isFinite(deltaVelocity)) {
            throw new IllegalArgumentException("deltaVelocity must be finite: " + deltaVelocity);
        }
        this.velocity += deltaVelocity;
    }

    /**
     * 解析解推进一帧（推荐入口）。
     *
     * <p>target 在 dt 内视为常量；开火时先 {@link #setTarget(float)} 再 update 即可。
     * 欠阻尼、临界阻尼、过阻尼三种情况分别有闭式解，所以不存在欧拉积分
     * “dt 越小越准”的问题，也不需要在 100Hz 里塞 substep。</p>
     */
    public void update(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }

        final double k = stiffness;
        final double c = damping;
        final double w = Math.sqrt(k);
        final double z = c / (2.0 * w);
        final double e0 = (double) position - target;
        final double v0 = velocity;
        final double t = dt;

        final double e;
        final double v;

        if (z < 1.0 - 1.0e-4) {
            // 欠阻尼：会超调，峰值由 ζ 决定。
            final double wd = w * Math.sqrt(1.0 - z * z);
            final double decay = Math.exp(-z * w * t);
            final double cos = Math.cos(wd * t);
            final double sin = Math.sin(wd * t);
            e = decay * (e0 * cos + (v0 + z * w * e0) / wd * sin);
            v = decay * (v0 * cos - (z * w * v0 + w * w * e0) / wd * sin);
        } else if (z > 1.0 + 1.0e-4) {
            // 过阻尼：两个实根，不会超调，但回得更慢。
            final double q = Math.sqrt(z * z - 1.0);
            final double r1 = -w * (z - q);
            final double r2 = -w * (z + q);
            final double c1 = (v0 - r2 * e0) / (r1 - r2);
            final double c2 = e0 - c1;
            final double e1 = Math.exp(r1 * t);
            final double e2 = Math.exp(r2 * t);
            e = c1 * e1 + c2 * e2;
            v = r1 * c1 * e1 + r2 * c2 * e2;
        } else {
            // 临界阻尼：t95 ≈ 4.74 / ω，单调逼近 target。
            final double decay = Math.exp(-w * t);
            e = (e0 + (v0 + w * e0) * t) * decay;
            v = (v0 - w * (v0 + w * e0) * t) * decay;
        }

        position = (float) (target + e);
        velocity = (float) v;
    }

    /**
     * 显式欧拉，和旧 {@code RecoilSpring1D.updateExplicit} 同构：
     * {@code a = k*(target-x) - c*v; v += a*dt; x += v*dt}。
     *
     * <p>只用于和现有手感做 A/B。k 大 / dt 大时会明显失真，甚至发散。</p>
     */
    public void updateEuler(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }
        float acceleration = stiffness * (target - position) - damping * velocity;
        velocity += acceleration * dt;
        position += velocity * dt;
    }

    /**
     * 半隐式欧拉（阻尼项隐式），和旧 {@code RecoilSpring1D.updateImplicit} 同构。
     * 只用于对比手感。
     */
    public void updateImplicit(float dt) {
        if (!(dt > 0f) || !Float.isFinite(dt)) {
            return;
        }
        velocity = (velocity + stiffness * (target - position) * dt) / (1f + damping * dt);
        position += velocity * dt;
    }

    /** 清空状态：position = 0，velocity = 0，target = 0。 */
    public void reset() {
        reset(0f, 0f);
    }

    /** 指定初始位置和目标位置，速度清零。 */
    public void reset(float position, float target) {
        if (!Float.isFinite(position)) {
            throw new IllegalArgumentException("position must be finite: " + position);
        }
        setTarget(target);
        this.position = position;
        this.velocity = 0f;
    }

    /** 把位置直接贴到 target，速度清零（切枪 / 重置时用）。 */
    public void snapToTarget() {
        this.position = target;
        this.velocity = 0f;
    }

    /** 位置和速度都进入给定误差带时返回 true。 */
    public boolean isSettled(float positionEpsilon, float velocityEpsilon) {
        return Math.abs(position - target) <= positionEpsilon
                && Math.abs(velocity) <= velocityEpsilon;
    }

    public float getTarget() {
        return target;
    }

    public float getPosition() {
        return position;
    }

    public float getVelocity() {
        return velocity;
    }

    /** 当前位置相对 target 的误差，等价于 position - target。 */
    public float getError() {
        return position - target;
    }

    public float getStiffness() {
        return stiffness;
    }

    public float getDamping() {
        return damping;
    }

    public float getOmega() {
        return (float) Math.sqrt(stiffness);
    }

    public float getZeta() {
        return (float) (damping / (2.0 * Math.sqrt(stiffness)));
    }

    private static void validateResponse(float omega, float zeta) {
        if (!(omega > 0f) || !Float.isFinite(omega)) {
            throw new IllegalArgumentException("omega must be finite and > 0: " + omega);
        }
        if (!(zeta >= 0f) || !Float.isFinite(zeta)) {
            throw new IllegalArgumentException("zeta must be finite and >= 0: " + zeta);
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "Spring1D[k=%.2f, c=%.2f, target=%.4f, x=%.4f, v=%.4f]",
                stiffness, damping, target, position, velocity);
    }
}
