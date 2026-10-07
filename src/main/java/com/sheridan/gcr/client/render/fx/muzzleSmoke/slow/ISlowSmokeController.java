package com.sheridan.gcr.client.render.fx.muzzleSmoke.slow;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * slow smoke "自身怎么变"的控制器抽象。
 * <p>
 * 取代了原来 {@link SlowSmokeEntry} 里写死的 expand / flyDir / flyDirRand / alpha：
 * 位置偏移、大小、透明度、是否取消全部由它按进度算出来，方便换不同的烟雾表现。
 * <p>
 * 只负责烟雾自身的变换；镜头与全局坐标（世界坐标）的换算由 {@link SlowSmokeTask} 负责，
 * 两者互不干扰：controller 给出的 {@link TransformRes#positionOffset()} 是<b>局部空间</b>偏移。
 */
@OnlyIn(Dist.CLIENT)
public interface ISlowSmokeController {
    /**
     * 计算某一帧烟雾自身的变换，把结果<b>复写</b>进 result
     * （result 由 task 持有并复用，实现里不要 new 对象、不要替换 result 里的 Vector3f）。
     *
     * @param progress 生命进度 0~1（1 = 已经淡出到看不见）
     * @param seed     该 task 的随机种子，同一个 task 内固定不变，用来做每个 task 自己的随机差异
     * @param result   输出：位置偏移（局部空间）/ alpha / scale / 是否取消
     */
    void update(float progress, int seed, TransformRes result);
}
