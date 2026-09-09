package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IGP25View;
import com.sheridan.gcr.modularSys.modules.views.IM203View;

public class GP25Viewer extends StateViewer<IGP25View> {
    public GP25Viewer(IGP25View view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping("base", "gcr:gp25_base", DEFAULT_SCALE, 0);
        addStateMapping("empty", "gcr:m203_empty", DEFAULT_SCALE, 0);
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        IGP25View stateView = getStateView();
        doPose("base", animated, context);
        String chamberStatus = stateView.getChamberStatus(states);
        if (IM203View.CHAMBER_EMPTY.equals(chamberStatus)) {
            doPose("empty", animated, context);
        }
    }
}
