package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.model.modular.state.StaticState;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARFullAuto;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARSemi;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.views.AKView;

public class AKViewer extends StateViewer<AKView> {
    public AKViewer(AKView stateView) {
        super(stateView);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping("base", "gcr:ak74m_base", DEFAULT_SCALE, 0);
        addStateMapping("stuck", "gcr:ak74m_shoot_stuck", DEFAULT_SCALE, 1);
        addStateMapping("stuck_empty", "gcr:ak74m_shoot_last_stuck", DEFAULT_SCALE, 1);

        addStateMappings(
                StaticState.Builder.empty(IGun.FIRE_MODEL_ID.getDefaultValue()),
                StaticState.Builder.of(ARSemi.SEMI.getName()).setRotation("safety", 90, 0, 0).build(),
                StaticState.Builder.of(ARFullAuto.FULL_AUTO.getName()).setRotation("safety", 180, 0, 0).build()
        );

        addStateMapping(StaticState.Builder.of("chamber_empty").setScale("ammo", 0).build());
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        AKView view = getStateView();

//        doPose(view.getFireModeId(states), animated, context);
//        if (view.boltLocked(states)) {
//            doPose("bolt_locked", animated, context);
//        } else if (view.stuck(states)) {
//            doPose("stuck", animated, context);
//        } else {
//
            doPose("base", animated, context);
            if (view.getAmmoLeft(states) <= 0) {
                doPose("chamber_empty", animated, context);
            }
//        }
    }
}
