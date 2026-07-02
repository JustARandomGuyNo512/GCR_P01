package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IM203View;

public class TestM203Viewer extends StateViewer<IM203View> {
    public TestM203Viewer(IM203View view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping("base", "gcr:m203_base", DEFAULT_SCALE, 0);
        addStateMapping("full", "gcr:m203_full", DEFAULT_SCALE, 0);
        addStateMapping("empty", "gcr:m203_empty", DEFAULT_SCALE, 0);
        addStateMapping("fired", "gcr:m203_fired", DEFAULT_SCALE, 0);
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        IM203View stateView = getStateView();
        doPose("base", animated, context);
        String chamberStatus = stateView.getChamberStatus(states);
        if (IM203View.CHAMBER_LOADED.equals(chamberStatus)) {
            doPose("full", animated, context);
        } else if (IM203View.CHAMBER_EMPTY.equals(chamberStatus)) {
            doPose("empty", animated, context);
        } else if (IM203View.CHAMBER_FIRED.equals(chamberStatus)) {
            doPose("fired", animated, context);
        }
    }
}
