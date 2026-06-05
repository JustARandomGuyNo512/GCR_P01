package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.builder.ShadowNode;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.states.State;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class StatesUpdateContext {
    public final IGun gun;
    public final ShadowNode root;
    private final Map<String, ShadowNode> idToNodes;

    private final CompoundTag statesData;

    private ShadowNode thisNode;
    private CompoundTag thisStatesData;


    public StatesUpdateContext(IGun gun, ShadowNode root, CompoundTag statesData) {
        this.gun = gun;
        this.root = root;
        this.thisNode = root;
        this.statesData = statesData;
        this.idToNodes = new HashMap<>();
        for (ShadowNode node : root.getAllNodesOfThisTree()) {
            idToNodes.put(node.nodeId, node);
        }
    }

    public ShadowNode getThisNode() {
        return thisNode;
    }

    public boolean thisNodeIsRoot() {
        return thisNode == root;
    }

    public ShadowNode getNodeById(String inTimeId) {
        return idToNodes.get(inTimeId);
    }

    public boolean hasNode(String inTimeId) {
        return idToNodes.containsKey(inTimeId);
    }

    public void autoExec() {
        Set<String> allEffectiveIds = new HashSet<>();
        for (ShadowNode node : root.getAllNodesOfThisTree()) {
            IModular module = node.unit.getModule();
            if (module instanceof IStateModular stateModular) {
                thisNode = node;
                if (statesData.contains(node.nodeId)) {
                    thisStatesData = statesData.getCompound(node.nodeId);
                } else {
                    thisStatesData = new CompoundTag();
                    stateModular.onInitStates(thisStatesData, node.nodeId, module.getID());
                }
                stateModular.writeNodeAndModuleId(thisStatesData, node.nodeId, module.getID());
                stateModular.onUpdate(this);
                statesData.put(node.nodeId, thisStatesData);
                allEffectiveIds.add(node.nodeId);
            }
        }
        statesData.getAllKeys().removeIf(key -> !allEffectiveIds.contains(key));
    }

    protected void reCalcProperties() {

    }

    public CompoundTag getStatesData(ShadowNode node) {
        return statesData.contains(node.nodeId) ?
                statesData.getCompound(node.nodeId) :
                new CompoundTag();
    }

    public String thisNodeId() {
        return thisNode.nodeId;
    }

    public List<ShadowNode> getAllNodesOfThisTree() {
        return root.getAllNodesOfThisTree();
    }

    public<T> void set(State<T> state, T value) {
        state.set(value, thisStatesData);
    }

    public <T> T get(State<T> state) {
        return state.get(thisStatesData);
    }
}
