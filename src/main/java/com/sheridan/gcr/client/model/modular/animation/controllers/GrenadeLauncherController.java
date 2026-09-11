package com.sheridan.gcr.client.model.modular.animation.controllers;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.model.modular.modules.GrenadeLauncherModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 榴弹发射器动画控制器的公共实现。
 *
 * <p>M203 与 GP25 的控制器除了动画资源路径以外完全一致：</p>
 * <ul>
 *     <li>{@code main} 轨道播放装填动画（排除 {@code shell} 骨头的状态覆盖）；</li>
 *     <li>{@code check} 轨道播放检视动画，仅在 main 空闲时触发，瞄准时立即清轨；</li>
 *     <li>开火时清掉检视轨道，避免检视姿态残留到射击动作上。</li>
 * </ul>
 *
 * <p>子类只需给出自己的检视/装填动画资源路径，并声明可兼容的模型类型。</p>
 *
 * <p><b>注意区分两个命名空间：</b>{@link #CHECK_ANIMATION} / {@link #RELOAD_ANIMATION}
 * 是注册进本地动画池的 <i>simple 别名</i>（{@code registerAnimations} 的第一个参数），
 * {@link #MAIN_TRACK} / {@link #CHECK_TRACK} 是 <i>轨道名</i>（{@code defineTrack} 的参数）。
 * 播放时 {@code anim(...)} 取的是别名，绝不能传全局资源路径。</p>
 *
 * @param <T> 该控制器绑定的榴弹发射器模型
 */
@OnlyIn(Dist.CLIENT)
public abstract class GrenadeLauncherController<T extends GrenadeLauncherModel<?>> extends AnimationController<T> {

    /** 本地动画别名：检视。 */
    protected static final String CHECK_ANIMATION = "check";
    /** 本地动画别名：装填。 */
    protected static final String RELOAD_ANIMATION = "reload";

    /** 轨道名：主（装填）动作。 */
    private static final String MAIN_TRACK = "main";
    /** 轨道名：检视动作。 */
    private static final String CHECK_TRACK = "check";

    /** 装填动画不覆盖弹壳骨头的状态，弹壳由状态视图按弹膛状态控制。 */
    private static final String SHELL_BONE = "shell";

    /** 检视动画的全局资源路径，注册为 {@link #CHECK_ANIMATION} 别名。 */
    protected abstract String getCheckAnimationPath();

    /** 装填动画的全局资源路径，注册为 {@link #RELOAD_ANIMATION} 别名。 */
    protected abstract String getReloadAnimationPath();

    @Override
    public void firstPersonSubscriptions(T model) {
        super.firstPersonSubscriptions(model);

        subscribe(EventType.CHECK_SUB_WEAPON, 0, (ctx) -> {
            if (isTrackClear(MAIN_TRACK)) {
                getTrack(CHECK_TRACK).play(anim(CHECK_ANIMATION));
            }
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 0, (ctx) -> {
            getTrack(MAIN_TRACK).play(anim(RELOAD_ANIMATION).coverStateExclude(SHELL_BONE));
        });

        subscribe(EventType.SHOOT, 0, (ctx) -> getTrack(CHECK_TRACK).clear());
    }

    @Override
    public void initAnimation(T model) {
        registerAnimations(
                CHECK_ANIMATION, getCheckAnimationPath(),
                RELOAD_ANIMATION, getReloadAnimationPath()
        );
    }

    @Override
    public void initTrack(T model) {
        defineTrack(MAIN_TRACK).addOnPlayed(instance -> getTrack(CHECK_TRACK).clear());
        defineTrack(CHECK_TRACK).addOnApplied((ctx, m) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack(CHECK_TRACK).clear();
            }
        });
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof GrenadeLauncherModel<?>;
    }
}
