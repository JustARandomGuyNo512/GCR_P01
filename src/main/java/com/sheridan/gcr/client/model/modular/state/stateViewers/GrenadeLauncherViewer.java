package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IGrenadeLauncherView;

/**
 * 榴弹发射器状态视图的公共部分。
 *
 * <p>所有型号都先套用 {@code base} 姿态，然后按弹膛状态叠加 {@link IGrenadeLauncherView#CHAMBER_EMPTY}
 * 对应的 {@code empty} 姿态（把榴弹骨头缩掉）。子类在
 * {@link #onRegisterStateMapping()} 里注册自己的状态映射，并按需扩展
 * {@link #applyState(IAnimated, ModuleRenderContext, ReadOnlyTag)} 追加型号特有的状态，
 * 例如 M203 的 {@code full} / {@code fired}。没有注册的状态会被 {@code doPose} 安全跳过。</p>
 */
public class GrenadeLauncherViewer<T extends IGrenadeLauncherView> extends StateViewer<T> {

    /** 基础姿态状态名。 */
    protected static final String BASE_STATE = "base";
    /** 空膛姿态状态名。 */
    protected static final String EMPTY_STATE = "empty";

    public GrenadeLauncherViewer(T view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        doPose(BASE_STATE, animated, context);
        if (IGrenadeLauncherView.CHAMBER_EMPTY.equals(getStateView().getChamberStatus(states))) {
            doPose(EMPTY_STATE, animated, context);
        }
    }
}
