package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.command.MaskMagAmmo;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;

import java.util.HashMap;
import java.util.Map;

public class CommonMagViewer extends StateViewer<IAmmoSourceView> {
    public int ammoModelCount;
    public String animationNamePrefix;
    public Map<Integer, String> ammoStateNames = new HashMap<>();

    public CommonMagViewer(IAmmoSourceView view, int ammoModelCount, String animationNamePrefix) {
        super(view);
        if (ammoModelCount < 2) {
            throw new IllegalArgumentException("Ammo model count must be at least 2");
        }
        this.ammoModelCount = ammoModelCount;
        this.animationNamePrefix = animationNamePrefix;
    }

    @Override
    public void onRegisterStateMapping() {
        ammoStateNames = new HashMap<>();
        for (int i = 1; i < ammoModelCount; i++) {
            addStateMapping("left_" + i, animationNamePrefix + "_left_" + i, DEFAULT_SCALE, 0);
            ammoStateNames.put(i, "left_" + i);
        }
        addStateMapping("full_1", animationNamePrefix + "_full_1", DEFAULT_SCALE, 0);
        addStateMapping("full_2", animationNamePrefix + "_full_2", DEFAULT_SCALE, 0);
        addStateMapping("empty", animationNamePrefix + "_empty", DEFAULT_SCALE, 0);
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        IAmmoSourceView stateView = getStateView();
        Integer maskAmmoLeft = context.getLocalStorage(MaskMagAmmo.MASK_AMMO_LEFT_KEY, Integer.class);
        int ammoLeft = maskAmmoLeft == null ? stateView.getAmmoLeft(states) : maskAmmoLeft;
        if (ammoLeft == 0) {
            doPose("empty", animated, context);
        } else if (ammoLeft >= 1 && ammoLeft < ammoModelCount) {
            String name = ammoStateNames.get(ammoLeft);
            if (name != null) {
                doPose(name, animated, context);
            }
        } else if (ammoLeft >= ammoModelCount) {
            if (ammoLeft % 2 == 0) {
                doPose("full_2", animated, context);
            } else {
                doPose("full_1", animated, context);
            }
        }
    }
}
