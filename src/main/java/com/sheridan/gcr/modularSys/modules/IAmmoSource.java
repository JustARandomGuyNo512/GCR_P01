package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.modules.states.Int;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import net.minecraft.nbt.CompoundTag;

public interface IAmmoSource extends IAmmoSourceView, IStateModular {
    Int AMMO_LEFT = new Int("ammo_left");

    @Override
    default void onInitStates(CompoundTag states, String nodeId, String moduleId) {
        AMMO_LEFT.init(states);
    }

    void setAmmoLeft(int ammoLeft, CompoundTag states);

    @Override
    default void onUpdate(StatesUpdateContext context) {}
}
