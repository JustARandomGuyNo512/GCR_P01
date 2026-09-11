package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IM203View;

/**
 * M203 的状态视图：在公共的 {@code base} / {@code empty} 之外，
 * 还有弹壳留在膛内的 {@code full} 与 {@code fired} 两个状态。
 */
public class M203Viewer extends GrenadeLauncherViewer<IM203View> {

    /** 弹壳在膛（已装填）。 */
    protected static final String FULL_STATE = "full";
    /** 已击发、弹壳未退出。 */
    protected static final String FIRED_STATE = "fired";

    public M203Viewer(IM203View view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping(BASE_STATE, "gcr:m203_base", DEFAULT_SCALE, 0);
        addStateMapping(FULL_STATE, "gcr:m203_full", DEFAULT_SCALE, 0);
        addStateMapping(EMPTY_STATE, "gcr:m203_empty", DEFAULT_SCALE, 0);
        addStateMapping(FIRED_STATE, "gcr:m203_fired", DEFAULT_SCALE, 0);
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        super.applyState(animated, context, states);
        String chamberStatus = getStateView().getChamberStatus(states);
        if (IM203View.CHAMBER_LOADED.equals(chamberStatus)) {
            doPose(FULL_STATE, animated, context);
        } else if (IM203View.CHAMBER_FIRED.equals(chamberStatus)) {
            doPose(FIRED_STATE, animated, context);
        }
    }
}
