package com.sheridan.gcr.modularSys.builder;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShadowNode {
    public Unit unit;
    public ShadowNode parent;
    private final Map<String, ShadowNode> children;
    public final String nodeId;
    public final boolean isRoot;
    private List<ShadowNode> allNodes;

    public ShadowNode(Unit unit, ShadowNode parent, String nodeId, boolean isRoot) {
        this.unit = unit;
        this.parent = parent;
        this.nodeId = nodeId;
        this.children = new HashMap<>();
        this.isRoot = isRoot;
        if (isRoot) {
            allNodes = new ArrayList<>();
        } else {
            allNodes = parent.allNodes;
        }
    }

    public List<ShadowNode> getAllNodesOfThisTree() {
        return allNodes;
    }

    public void addChild(ShadowNode child) {
        children.put(child.nodeId, child);
    }

    public ShadowNode getChild(String nodeId) {
        return children.get(nodeId);
    }

    public void complete() {
        if (isRoot) {
            this.allNodes = ImmutableList.copyOf(allNodes);
            for (ShadowNode node : allNodes) {
                if (node == this) {
                    continue;
                }
                node.allNodes = this.allNodes;
            }
        }
    }
}
