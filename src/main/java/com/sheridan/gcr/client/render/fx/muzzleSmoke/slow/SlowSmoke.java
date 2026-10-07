package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * slow muzzle smoke 的贴图集合持有者。
 * <p>
 * 只负责"有哪些贴图可用"（{@link SlowSmokeTextureSet}：贴图列表 + 基础大小 / 基础透明度），
 * 本身不保存任何 {@link SlowSmokeEntry} 参数——fadeSeconds / localScale / expand / flyDir / flyDirRand
 * 由具体 {@code MuzzleEntry} 通过 {@code withSlowSmokeEntry(...)} 配置，这样每条枪口都能单独调参。
 * <p>
 * 作为静态预设时（{@code CommonSlowSmokeEffects}）它就等价于一份可复用的贴图集合。
 */
@OnlyIn(Dist.CLIENT)
public class SlowSmoke {
    private final SlowSmokeTextureSet textureSet;

    public SlowSmoke(SlowSmokeTextureSet textureSet) {
        this.textureSet = textureSet;
    }

    public SlowSmokeTextureSet getTextureSet() {
        return textureSet;
    }

    /** 可用的贴图数量 */
    public int textureCount() {
        return textureSet == null ? 0 : textureSet.size();
    }

    /**
     * 这次射击的 slow smoke 是否已经淡出结束。
     * <p>
     * 用于避免枪口的时间戳已经过期（但 {@code GunEffectManager} 还留着旧值）时反复创建 task。
     *
     * @param entry 该 MuzzleEntry 配置的参数，null 表示用 {@link SlowSmokeEntry} 的默认值
     */
    public boolean isExpired(long shootTime, @Nullable SlowSmokeEntry entry) {
        return System.currentTimeMillis() - shootTime >= durationMillis(entry);
    }

    /** 创建一次射击的 smoke task，位置矩阵在构造函数里会被复制一份 */
    public SlowSmokeTask createTask(PoseStack.Pose pose, long shootTime) {
        return createTask(pose, shootTime, null, LightTexture.FULL_BRIGHT);
    }

    public SlowSmokeTask createTask(PoseStack.Pose pose, long shootTime, int light) {
        return createTask(pose, shootTime, null, light);
    }

    /**
     * @param entry 该 MuzzleEntry 上配置的 {@link SlowSmokeEntry}，null 时用默认参数
     */
    public SlowSmokeTask createTask(PoseStack.Pose pose, long shootTime, @Nullable SlowSmokeEntry entry, int light) {
        SlowSmokeEntry config = entry == null ? new SlowSmokeEntry() : entry;
        return new SlowSmokeTask(pose, shootTime, config, textureSet, light);
    }

    private static int durationMillis(@Nullable SlowSmokeEntry entry) {
        return entry == null ? new SlowSmokeEntry().fadeDurationMillis() : entry.fadeDurationMillis();
    }
}
