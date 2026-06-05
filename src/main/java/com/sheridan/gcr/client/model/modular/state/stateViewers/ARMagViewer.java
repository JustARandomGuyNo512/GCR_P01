package com.sheridan.gcr.client.model.modular.state.stateViewers;

import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.command.MaskMagAmmo;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.modular.state.StateViewer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;

public class ARMagViewer extends StateViewer<IAmmoSourceView> {
    public ARMagViewer(IAmmoSourceView view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping("full_1", "gcr:ar_mag_30r_full_1", DEFAULT_SCALE, 0);
        addStateMapping("full_2", "gcr:ar_mag_30r_full_2", DEFAULT_SCALE, 0);
        addStateMapping("left_1", "gcr:ar_mag_30r_left_1", DEFAULT_SCALE, 0);
        addStateMapping("left_2", "gcr:ar_mag_30r_left_2", DEFAULT_SCALE, 0);
        addStateMapping("left_3", "gcr:ar_mag_30r_left_3", DEFAULT_SCALE, 0);
        addStateMapping("empty", "gcr:ar_mag_30r_empty", DEFAULT_SCALE, 0);
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        IAmmoSourceView stateView = getStateView();
        Integer maskAmmoLeft = context.getLocalStorage(MaskMagAmmo.MASK_AMMO_LEFT_KEY, Integer.class);
        int ammoLeft = maskAmmoLeft == null ? stateView.getAmmoLeft(states) : maskAmmoLeft;
        if (ammoLeft == 0) {
            doPose("empty", animated, context);
        } else if (ammoLeft == 1) {
            doPose("left_1", animated, context);
        } else if (ammoLeft == 2) {
            doPose("left_2", animated, context);
        } else if (ammoLeft == 3) {
            doPose("left_3", animated, context);
        } else if (ammoLeft > 3) {
            if (ammoLeft % 2 == 0) {
                doPose("full_2", animated, context);
            } else {
                doPose("full_1", animated, context);
            }
        }
    }
}
