package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * {@link ISlowSmokeController} 的输出：一帧里烟雾自身的变换。
 * <p>
 * 由 {@link SlowSmokeTask} 持有并复用，controller 每帧<b>复写</b>里面的字段，
 * 不要每帧 new 这个对象（省 GC）。
 * <ul>
 *     <li>{@link #positionOffset()}：位置偏移，局部空间（相机对齐，-Z 前 / +X 右），单位/秒那种"位移量"，不是速度</li>
 *     <li>{@link #alpha()}：透明度（0~1），最终还会乘上贴图自己的基础透明度</li>
 *     <li>{@link #scale()}：大小倍率，最终乘上贴图基础大小与 {@link SlowSmokeEntry#localScale()}</li>
 *     <li>{@link #isCancel()}：true 表示这一条烟雾立刻取消（task 会被移除，不再渲染）</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class TransformRes {
    private final Vector3f positionOffset = new Vector3f();
    private float alpha = 1f;
    private float scale = 1f;
    private boolean cancel;

    /** 位置偏移（局部空间）。直接 {@code positionOffset().set(...)} 复写，不要替换这个对象 */
    public Vector3f positionOffset() {
        return positionOffset;
    }

    public float alpha() {
        return alpha;
    }

    public TransformRes setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public float scale() {
        return scale;
    }

    public TransformRes setScale(float scale) {
        this.scale = scale;
        return this;
    }

    public boolean isCancel() {
        return cancel;
    }

    public TransformRes setCancel(boolean cancel) {
        this.cancel = cancel;
        return this;
    }

    /** 一次性复写全部字段（controller 每帧调用，避免漏设字段导致残留上一帧的值） */
    public TransformRes set(float offsetX, float offsetY, float offsetZ, float alpha, float scale, boolean cancel) {
        this.positionOffset.set(offsetX, offsetY, offsetZ);
        this.alpha = alpha;
        this.scale = scale;
        this.cancel = cancel;
        return this;
    }
}
