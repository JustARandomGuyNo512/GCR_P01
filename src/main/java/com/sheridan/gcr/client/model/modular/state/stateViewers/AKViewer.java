package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.model.modular.state.StaticState;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.fire.closedBolt.AKFullAuto;
import com.sheridan.gcr.modularSys.fire.closedBolt.AKSemi;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.views.AKView;

public class AKViewer extends StateViewer<AKView> {
    private String baseSamplerAnimation;
    private String stuckSamplerAnimation;
    private String semiSamplerAnimation;
    private String fullAutoSamplerAnimation;

    public AKViewer(AKView stateView, String baseSamplerAnimation, String stuckSamplerAnimation, String semiSamplerAnimation, String fullAutoSamplerAnimation) {
        super(stateView);
        this.baseSamplerAnimation = baseSamplerAnimation;
        this.stuckSamplerAnimation = stuckSamplerAnimation;
        this.semiSamplerAnimation = semiSamplerAnimation;
        this.fullAutoSamplerAnimation = fullAutoSamplerAnimation;
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping("base", baseSamplerAnimation, DEFAULT_SCALE, 0);
        addStateMapping("stuck", stuckSamplerAnimation, DEFAULT_SCALE, 1);
        addStateMapping(AKSemi.SEMI.getName(), semiSamplerAnimation, DEFAULT_SCALE, 1);
        addStateMapping(AKFullAuto.FULL_AUTO.getName(), fullAutoSamplerAnimation, DEFAULT_SCALE, 1);
        addStateMapping(StaticState.Builder.empty(IGun.FIRE_MODEL_ID.getDefaultValue()));
        addStateMapping(StaticState.Builder.of("chamber_empty").setScale("ammo", 0).build());
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        AKView view = getStateView();
        doPose(view.getFireModeId(states), animated, context);
        if (view.stuck(states)) {
            doPose("stuck", animated, context);
        } else {
            doPose("base", animated, context);
        }
        if (view.getAmmoLeft(states) <= 0) {
            doPose("chamber_empty", animated, context);
        }

    }
}
