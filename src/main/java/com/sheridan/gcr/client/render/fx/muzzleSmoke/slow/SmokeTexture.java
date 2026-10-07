package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

/**
 * slow muzzle smoke 的单张贴图配置。
 * <p>
 * 与 {@code FastMuzzleSmoke} 不同，slow smoke 的每张图都不是序列图，不做 UV 分块，
 * 渲染时整张图当成一个 quad 使用，所以这里只有贴图位置与该张图自己的基础大小 / 基础透明度。
 * <p>
 * 最终渲染时：大小 = {@link #baseSize()} * {@link SlowSmokeEntry#localScale()}，
 * 透明度 = {@link #baseAlpha()} *  * 淡出系数。
 */
@OnlyIn(Dist.CLIENT)
public final class SmokeTexture {
    private final ResourceLocation location;
    private float baseSize = 1f;
    private float baseAlpha = 1f;

    public SmokeTexture(ResourceLocation location) {
        this.location = location;
    }

    public ResourceLocation location() {
        return location;
    }

    /**
     * 这张贴图的基础大小，渲染时与 {@link SlowSmokeEntry#localScale()} 相乘
     */
    public float baseSize() {
        return baseSize;
    }

    /**
     * 这张贴图的基础透明度，渲染时与 {@link SlowSmokeEntry} 相乘
     */
    public float baseAlpha() {
        return baseAlpha;
    }

    public SmokeTexture setBaseSize(float baseSize) {
        this.baseSize = Math.max(0f, baseSize);
        return this;
    }

    public SmokeTexture setBaseAlpha(float baseAlpha) {
        this.baseAlpha = Math.max(0f, baseAlpha);
        return this;
    }

    /**
     * 同时链式设置基础大小与基础透明度
     */
    public SmokeTexture set(float baseSize, float baseAlpha) {
        return setBaseSize(baseSize).setBaseAlpha(baseAlpha);
    }

    /**
     * 用 Vector2f(size, alpha) 同时链式设置基础大小与基础透明度
     */
    public SmokeTexture set(Vector2f sizeAndAlpha) {
        return set(sizeAndAlpha.x, sizeAndAlpha.y);
    }

    public SmokeTexture copy() {
        return new SmokeTexture(location).set(baseSize, baseAlpha);
    }
}
