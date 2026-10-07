package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 通用的 slow smoke 控制器：
 * <ul>
 *     <li><b>scale</b>：先快速变大（模拟炸开），然后越来越慢地继续变大</li>
 *     <li><b>positionOffset</b>：先快速向前（-Z），随后位移速度逐渐变慢，同时方向慢慢转向 +X 飘散</li>
 *     <li><b>alpha</b>：前期保持不变，progress 超过 {@link #fadeStart()} 之后越来越快地淡出</li>
 * </ul>
 * 用 task 的 seed 给每个 task 加一点差异（位移总量 / 大小 / 转向幅度 / 轻微的上下偏移 /
 * z 轴随机错开，避免连发的多个 quad 共面 z-fighting），
 * 同一个 task 每帧结果稳定，不会抖动。
 * <p>
 * 输出通过 {@link TransformRes} 复写，update 里不产生任何新对象。
 */
@OnlyIn(Dist.CLIENT)
public class CommonSlowSmokeController implements ISlowSmokeController {
    // ── 大小：先快后慢 ──────────────────────────────────────────────
    private float startScale = 0.45f;
    private float endScale = 2.2f;
    /** 越大 => 前期涨得越快、后期越慢 */
    private float growCurve = 3f;
    /** 每个 task 的大小随机浮动（0.25 = ±25%） */
    private float scaleRandom = 0.15f;

    // ── 位置：先前冲、后变慢并转向 +X ─────────────────────────────────
    /** 整个生命周期飘散的总距离（局部空间单位） */
    private float travel = 1f;
    /** 位移的先快后慢程度，越大前期冲得越猛 */
    private float travelCurve = 2f;
    /** 转向 +X 的速度，越大越早开始转 */
    private float turnCurve = 1.6f;
    /** 基础向上分量（0 = 完全水平，只做 前 → +X 的转向） */
    private float rise = 0f;
    /** 每个 task 的位移总量随机浮动 */
    private float travelRandom = 0.25f;
    /** 每个 task 的转向幅度随机浮动 */
    private float turnRandom = 0.25f;
    /** 每个 task 的上下随机浮动 */
    private float riseRandom = 0.15f;
    /**
     * 每个 task 在局部 z 轴（枪口前后方向）上的随机基础偏移，单位是局部空间单位。
     * <p>
     * 连发时多个烟雾 quad、以及枪口火焰都在同一个枪口平面上，共面会 z-fighting，
     * 这里让每个 task 的位置沿 z 错开一点；偏移只往枪口前方（-Z）走，
     * 这样烟雾不会被推到火焰前面去盖住火焰。
     */
    private float zRandom = 0.1f;

    // ── 透明度：前期不变，后半段加速淡出 ───────────────────────────────
    private float startAlpha = 1f;
    /** 超过这个进度才开始淡出 */
    private float fadeStart = 0.5f;
    /** 越大后期掉得越快 */
    private float fadeCurve = 2f;

    @Override
    public void update(float progress, int seed, TransformRes result) {
        float p = Mth.clamp(progress, 0f, 1f);

        // ── scale：1-(1-p)^growCurve 的斜率随 p 递减 => 先快速变大，再慢慢变大 ──
        float grow = 1f - (float) Math.pow(1f - p, growCurve);
        float scale = Mth.lerp(grow, startScale, endScale) * around(random01(seed, 7919), scaleRandom);

        // ── 位置：位移先快后慢 ─────────────────────────────────────────
        float distance = travel * (1f - (float) Math.pow(1f - p, travelCurve))
                * around(random01(seed, 104729), travelRandom);

        // 方向从 (0, rise, -1) 平滑转到 (1, rise, 0)：turn 越大越偏向 +X
        float turn = Mth.clamp((float) Math.pow(p, turnCurve) * around(random01(seed, 15485863), turnRandom), 0f, 1f);
        float dirZ = -(1f - turn);
        float dirY = rise + (random01(seed, 32452843) - 0.5f) * 2f * riseRandom;
        float len = Mth.sqrt(turn * turn + dirY * dirY + dirZ * dirZ);
        if (len < 1.0E-6f) {
            len = 1f;
        }
        float offsetScale = distance / len;

        // 每个 task 的 z 轴随机基础偏移：往枪口前方错开 [zRandom*0.25, zRandom]，避免和别的 quad 共面 z-fighting
        float baseZ = -(0.25f + random01(seed, 86028121) * 0.75f) * zRandom;

        // ── alpha：前期不变，fadeStart 之后用 p^fadeCurve 加速淡出 ──────────
        float alpha = startAlpha;
        if (p > fadeStart) {
            float t = (p - fadeStart) / (1f - fadeStart);
            alpha = startAlpha * (1f - (float) Math.pow(t, fadeCurve));
        }

        result.set(turn * offsetScale, dirY * offsetScale, dirZ * offsetScale + baseZ,
                Mth.clamp(alpha, 0f, 1f), Math.max(0f, scale), false);
    }

    // ───────────────────────── 链式配置 ─────────────────────────

    /** 起始 / 结束大小倍率 */
    public CommonSlowSmokeController setScale(float startScale, float endScale) {
        this.startScale = Math.max(0f, startScale);
        this.endScale = Math.max(0f, endScale);
        return this;
    }

    /** 大小曲线：越大前期涨得越快 */
    public CommonSlowSmokeController setGrowCurve(float growCurve) {
        this.growCurve = Math.max(0.01f, growCurve);
        return this;
    }

    /** 飘散总距离（局部空间单位） */
    public CommonSlowSmokeController setTravel(float travel) {
        this.travel = Math.max(0f, travel);
        return this;
    }

    /** 位移曲线：越大前期冲得越猛 */
    public CommonSlowSmokeController setTravelCurve(float travelCurve) {
        this.travelCurve = Math.max(0.01f, travelCurve);
        return this;
    }

    /** 转向 +X 的速度：越大越早转向 */
    public CommonSlowSmokeController setTurnCurve(float turnCurve) {
        this.turnCurve = Math.max(0.01f, turnCurve);
        return this;
    }

    /** 基础向上分量（0 = 只做水平的前 → +X 转向） */
    public CommonSlowSmokeController setRise(float rise) {
        this.rise = rise;
        return this;
    }

    /** 起始透明度与开始淡出的进度 */
    public CommonSlowSmokeController setAlpha(float startAlpha, float fadeStart) {
        this.startAlpha = Mth.clamp(startAlpha, 0f, 1f);
        this.fadeStart = Mth.clamp(fadeStart, 0f, 1f);
        return this;
    }

    /** 淡出曲线：越大后期掉得越快 */
    public CommonSlowSmokeController setFadeCurve(float fadeCurve) {
        this.fadeCurve = Math.max(0.01f, fadeCurve);
        return this;
    }

    /** 每个 task 的随机浮动幅度（位移总量 / 大小 / 转向 / 上下） */
    public CommonSlowSmokeController setRandom(float travelRandom, float scaleRandom, float turnRandom, float riseRandom) {
        this.travelRandom = Math.max(0f, travelRandom);
        this.scaleRandom = Math.max(0f, scaleRandom);
        this.turnRandom = Math.max(0f, turnRandom);
        this.riseRandom = Math.max(0f, riseRandom);
        return this;
    }

    /**
     * 每个 task 在 z 轴（枪口前后）上的随机错开量，越大越不容易和别的 quad 共面 z-fighting
     * （偏移只往枪口前方 -Z 走，不会盖住枪口火焰）
     */
    public CommonSlowSmokeController setZRandom(float zRandom) {
        this.zRandom = Math.max(0f, zRandom);
        return this;
    }

    public float zRandom() {
        return zRandom;
    }

    public float startScale() {
        return startScale;
    }

    public float endScale() {
        return endScale;
    }

    public float travel() {
        return travel;
    }

    public float startAlpha() {
        return startAlpha;
    }

    public float fadeStart() {
        return fadeStart;
    }

    /** 围绕 1 的随机倍率：amount = 0 时恒为 1 */
    private static float around(float seed01, float amount) {
        return 1f + (seed01 - 0.5f) * 2f * amount;
    }

    /** 由种子推导 0..1 的稳定随机值（同一个 task 每帧都一样） */
    private static float random01(int seed, int salt) {
        int h = seed * 0x9E3779B9 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return (h >>> 8) / (float) 0x1000000;
    }
}
