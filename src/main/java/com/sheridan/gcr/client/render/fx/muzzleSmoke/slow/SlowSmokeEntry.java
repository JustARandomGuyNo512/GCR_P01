package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * slow muzzle smoke 的参数配置。
 * <p>
 * 保存：
 * <ul>
 *     <li>{@link #fadeSeconds()}：多少秒淡出直到完全看不见</li>
 *     <li>{@link #localScale()}：可调的局部大小，渲染时与贴图基础大小相乘</li>
 *     <li>{@link #controller()}：控制烟雾自身怎么变（位置偏移 / alpha / scale / 取消），见 {@link ISlowSmokeController}</li>
 * </ul>
 * 贴图相关配置在 {@link SlowSmokeTextureSet} 上；本类只描述"这一次飘散怎么动"，
 * 因此它是配置在具体 {@code MuzzleEntry} 上的（每把枪一套参数 + 一个 controller），
 * 而不是塞进 {@link SlowSmoke} 里。
 */
@OnlyIn(Dist.CLIENT)
public final class SlowSmokeEntry {
    /** 默认淡出时长（秒） */
    public static final float DEFAULT_FADE_SECONDS = 2.5f;

    private float fadeSeconds = DEFAULT_FADE_SECONDS;
    private float localScale = 1f;
    private boolean followFlyDir = true;
    private boolean randomRotate = true;
    /** 烟雾自身的变换控制器，每个 entry 默认一份自己的实例 */
    private ISlowSmokeController controller = new CommonSlowSmokeController();
    private SlowSmoke slowSmoke;

    public SlowSmokeEntry() {
    }

    /**
     * @param fadeSeconds 多少秒淡出直到完全看不见
     * @param localScale  局部大小，渲染时与贴图基础大小相乘
     * @param slowSmoke   这次用的贴图集合
     */
    public SlowSmokeEntry(float fadeSeconds, float localScale, SlowSmoke slowSmoke) {
        setFadeSeconds(fadeSeconds);
        setLocalScale(localScale);
        this.slowSmoke = slowSmoke;
    }

    /**
     * @param fadeSeconds 多少秒淡出直到完全看不见
     * @param localScale  局部大小，渲染时与贴图基础大小相乘
     * @param controller  烟雾自身的变换控制器
     * @param slowSmoke   这次用的贴图集合
     */
    public SlowSmokeEntry(float fadeSeconds, float localScale, ISlowSmokeController controller, SlowSmoke slowSmoke) {
        this(fadeSeconds, localScale, slowSmoke);
        setController(controller);
    }

    public SlowSmoke getSlowSmoke() {
        return slowSmoke;
    }

    public SlowSmokeEntry setSlowSmoke(SlowSmoke slowSmoke) {
        this.slowSmoke = slowSmoke;
        return this;
    }

    // ───────────────────────── 链式 builder ─────────────────────────

    /** 链式设置淡出时长（秒） */
    public SlowSmokeEntry fade(float seconds) {
        return setFadeSeconds(seconds);
    }

    /** 链式设置局部大小 */
    public SlowSmokeEntry scale(float localScale) {
        return setLocalScale(localScale);
    }

    /** 链式设置烟雾自身的变换控制器；传 null 会用默认的 {@link CommonSlowSmokeController} */
    public SlowSmokeEntry controller(ISlowSmokeController controller) {
        return setController(controller);
    }

    // ───────────────────────── 常规 getter / setter ─────────────────────────

    /**
     * 烟雾自身的变换控制器：位置偏移 / alpha / scale / 是否取消都由它按 progress 算出来
     */
    public ISlowSmokeController controller() {
        return controller;
    }

    public SlowSmokeEntry setController(ISlowSmokeController controller) {
        this.controller = controller == null ? new CommonSlowSmokeController() : controller;
        return this;
    }

    /** 淡出总时长（秒） */
    public float fadeSeconds() {
        return fadeSeconds;
    }

    /** 淡出总时长（毫秒）；进度 progress = 已存活时间 / 这个值 */
    public int fadeDurationMillis() {
        return Math.max(1, Math.round(fadeSeconds * 1000f));
    }

    public SlowSmokeEntry setFadeSeconds(float seconds) {
        this.fadeSeconds = Math.max(0.001f, seconds);
        return this;
    }

    /** 淡出总时长，单位毫秒 */
    public SlowSmokeEntry setFadeDurationMillis(int millis) {
        return setFadeSeconds(millis * 0.001f);
    }

    public float localScale() {
        return localScale;
    }

    public SlowSmokeEntry setLocalScale(float localScale) {
        this.localScale = Math.max(0f, localScale);
        return this;
    }

    /** 是否应用 controller 给出的位置偏移（false = 烟雾停在枪口不飘） */
    public boolean followFlyDir() {
        return followFlyDir;
    }

    public SlowSmokeEntry setFollowFlyDir(boolean followFlyDir) {
        this.followFlyDir = followFlyDir;
        return this;
    }

    /** 每个 task 是否随机自转 */
    public boolean randomRotate() {
        return randomRotate;
    }

    public SlowSmokeEntry setRandomRotate(boolean randomRotate) {
        this.randomRotate = randomRotate;
        return this;
    }

    /**
     * 复制一份参数；controller 按引用共享（想每个 entry 独立调参就用 {@code new CommonSlowSmokeController()} 换掉）
     */
    public SlowSmokeEntry copy() {
        SlowSmokeEntry entry = new SlowSmokeEntry(fadeSeconds, localScale, controller, slowSmoke);
        entry.followFlyDir = followFlyDir;
        entry.randomRotate = randomRotate;
        return entry;
    }
}
