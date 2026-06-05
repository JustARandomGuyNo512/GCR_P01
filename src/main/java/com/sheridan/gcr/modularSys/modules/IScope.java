package com.sheridan.gcr.modularSys.modules;


import com.sheridan.gcr.modularSys.modules.states.Num;
import com.sheridan.gcr.modularSys.modules.views.IScopeView;
import net.minecraft.nbt.CompoundTag;


public interface IScope extends ISight, IStateModular, IScopeView, IInteractiveModular {

    Num ZOOM_RATIO = new Num("zoom_ratio", 0);

    @Override
    default void onInitStates(CompoundTag states, String nodeId, String moduleId) {
        ZOOM_RATIO.init(states);
    }

    @Override
    default float getZCompensation() {
        return 0.3f;
    }

    void setZoomRatio(float ratio, CompoundTag states);

    float getZoomSensitivity();
}
