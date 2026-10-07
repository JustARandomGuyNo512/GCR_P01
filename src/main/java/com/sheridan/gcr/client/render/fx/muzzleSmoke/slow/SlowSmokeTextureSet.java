package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * slow muzzle smoke 的贴图集合，负责配置“一共有多少张贴图可用”。
 * <p>
 * 链式 setter（{@link #setBaseSize(float)} / {@link #setBaseAlpha(float)} / {@link #set(float, float)}）
 * 会同时写入集合内已有的每一张贴图；单张贴图也可以用 {@link SmokeTexture#set(float, float)} 单独覆盖。
 * <p>
 * 渲染时由 {@link SlowSmokeTask} 用自己那次的随机种子从集合里选出一个 index，
 * 同一个 task 每帧只会渲染这一张贴图一次，不会把集合里所有贴图都画一遍。
 */
@OnlyIn(Dist.CLIENT)
public final class SlowSmokeTextureSet {
    private final List<SmokeTexture> textures = new ArrayList<>();
    private float baseSize = 1f;
    private float baseAlpha = 1f;

    public SlowSmokeTextureSet() {
    }

    public SlowSmokeTextureSet(ResourceLocation... locations) {
        add(locations);
    }

    public SlowSmokeTextureSet add(ResourceLocation location) {
        textures.add(new SmokeTexture(location).set(baseSize, baseAlpha));
        return this;
    }

    public SlowSmokeTextureSet add(ResourceLocation... locations) {
        for (ResourceLocation location : locations) {
            add(location);
        }
        return this;
    }

    public SlowSmokeTextureSet add(SmokeTexture texture) {
        textures.add(texture);
        return this;
    }

    public int size() {
        return textures.size();
    }

    public boolean isEmpty() {
        return textures.isEmpty();
    }

    public SmokeTexture get(int index) {
        return textures.get(index);
    }

    public List<SmokeTexture> textures() {
        return textures;
    }

    public float baseSize() {
        return baseSize;
    }

    public float baseAlpha() {
        return baseAlpha;
    }

    /**
     * 链式设置基础大小，同时写入当前已有的每一张贴图
     */
    public SlowSmokeTextureSet setBaseSize(float baseSize) {
        this.baseSize = Math.max(0f, baseSize);
        for (SmokeTexture texture : textures) {
            texture.setBaseSize(this.baseSize);
        }
        return this;
    }

    /**
     * 链式设置基础透明度，同时写入当前已有的每一张贴图
     */
    public SlowSmokeTextureSet setBaseAlpha(float baseAlpha) {
        this.baseAlpha = Math.max(0f, baseAlpha);
        for (SmokeTexture texture : textures) {
            texture.setBaseAlpha(this.baseAlpha);
        }
        return this;
    }

    /**
     * 同时链式设置基础大小与基础透明度
     */
    public SlowSmokeTextureSet set(float baseSize, float baseAlpha) {
        return setBaseSize(baseSize).setBaseAlpha(baseAlpha);
    }

    /**
     * 用 Vector2f(size, alpha) 同时链式设置基础大小与基础透明度
     */
    public SlowSmokeTextureSet set(Vector2f sizeAndAlpha) {
        return set(sizeAndAlpha.x, sizeAndAlpha.y);
    }

    /**
     * 取第 index 张贴图，index 会被限制在可用范围内
     */
    public SmokeTexture pick(int index) {
        if (textures.isEmpty()) {
            return null;
        }
        return textures.get(Mth.clamp(index, 0, textures.size() - 1));
    }

    /**
     * 按随机种子取一张贴图，保证同一个 task 拿到的永远是同一张
     */
    public SmokeTexture pickBySeed(int seed) {
        if (textures.isEmpty()) {
            return null;
        }
        return pick(Math.floorMod(seed, textures.size()));
    }

    /**
     * 由随机种子推导出一个可用范围内的贴图 index
     */
    public int indexBySeed(int seed) {
        return textures.isEmpty() ? 0 : Math.floorMod(seed, textures.size());
    }

    /**
     * 复制一份，便于在共享配置的基础上派生单个枪械的配置
     */
    public SlowSmokeTextureSet copy() {
        SlowSmokeTextureSet set = new SlowSmokeTextureSet();
        set.baseSize = baseSize;
        set.baseAlpha = baseAlpha;
        for (SmokeTexture texture : textures) {
            set.textures.add(texture.copy());
        }
        return set;
    }
}
