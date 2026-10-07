package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import com.sheridan.gcr.GCR;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 常见 slow muzzle smoke 预设。
 * <p>
 * 这里只注册"有哪些贴图可用"（{@link SlowSmokeTextureSet}：贴图列表 + 基础大小 / 基础透明度），
 * 具体怎么飘（fadeSeconds / localScale / controller）在各自的 {@code MuzzleEntry} 上用
 * {@link SlowSmokeEntry} 单独配置，
 * 例如 {@code entry.withSlowSmoke(COMMON, new SlowSmokeEntry().fade(2.5f).scale(1.4f).controller(new CommonSlowSmokeController()))}。
 */
@OnlyIn(Dist.CLIENT)
public class CommonSlowSmokeEffects {
    /**
     * 测试用贴图集合：单张 64x64 的 smoke_0.png，整图当做一个 quad 渲染。
     * <p>
     * 想扩充可用贴图，直接继续 {@code .add(...)} 即可；
     * 渲染时会按 task 的随机种子从集合里随机挑一张（每帧只画一张）。
     */
    public static final SlowSmoke COMMON = new SlowSmoke(
            new SlowSmokeTextureSet()
                    // 基础大小 / 基础透明度在集合级别链式设置，会写入集合内每一张贴图
                    .set(1.5f, 0.8f)
                    .add(GCR.RL("textures/fx/slow_smoke/smoke_0.png")));
}
