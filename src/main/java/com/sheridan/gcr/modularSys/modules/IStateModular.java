package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.modules.states.Str;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.minecraft.nbt.CompoundTag;

public interface IStateModular extends IStateView {
    Str NODE_ID = new Str("node_id");
    Str MODULE_ID = new Str("module_id");

    void onInitStates(CompoundTag states, String nodeId, String moduleId);

    void onUpdate(StatesUpdateContext context);

    default void writeNodeAndModuleId(CompoundTag states, String nodeId, String moduleId) {
        NODE_ID.set(nodeId, states);
        MODULE_ID.set(moduleId, states);
    }

    @Override
    default String getNodeId(CompoundTag states) {
        return NODE_ID.get(states);
    }

    @Override
    default String getModuleId(CompoundTag states) {
        return MODULE_ID.get(states);
    }
}
