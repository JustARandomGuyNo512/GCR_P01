package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.model.modular.state.StaticState;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;

public class TestM203Viewer extends StateViewer<IAmmoSourceView> {
    public TestM203Viewer(IAmmoSourceView view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping(StaticState.Builder.of("base").setScale("grenade_reloading", 0).build());
        addStateMapping(StaticState.Builder.of("full").setScale("shell", 0).build());
        addStateMapping(StaticState.Builder.of("empty").setScale("raw", 0).build());

    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        IAmmoSourceView stateView = getStateView();
        doPose("base", animated, context);
        if (stateView.getAmmoLeft(states) <= 0) {
            doPose("empty", animated, context);
        } else {
            doPose("full", animated, context);
        }
    }
}
