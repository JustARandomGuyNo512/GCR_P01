package com.sheridan.gcr.modularSys.slot;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MLokRail extends Rail {

    // 保存归一化后的离散点位置 [0.0 ~ 1.0]
    private final float[] discreteNodes;


    public static MLokRail of(String name, Direction direction, float rearEndZ, float pivotZ, float farEndZ, float firstNode, float nodeStep, int nodeCount, String... tags) {
        if (firstNode > rearEndZ) {
            throw new IllegalArgumentException("RearEndZ offset must be less than firstNode offset");
        }
        if (nodeCount < 1) {
            throw new IllegalArgumentException("Node count must be greater than 0");
        }

        float [] nodes = new float[nodeCount];
        for (int i = 0; i < nodeCount; i ++) {
            float nodePos = (firstNode - i * nodeStep);
            nodes[i] = nodePos;
        }
        System.out.println("gen nodes: " + Arrays.toString(nodes));
        List<String> tagsList = new ArrayList<>();
        tagsList.add("m_lok_rail");
        tagsList.addAll(Arrays.asList(tags));
        String[] array = tagsList.toArray(new String[0]);
        System.out.println("gen tags: " + Arrays.toString(array));
        return new MLokRail(name, direction, rearEndZ, pivotZ, farEndZ, nodes, array);
    }

    public MLokRail(String name, Direction direction, float rearEndZ, float pivotZ, float farEndZ, float[] nodes, String... tags) {
        super(name, direction, rearEndZ, pivotZ, farEndZ, tags);
        if (nodes == null || nodes.length == 0) {
            throw new IllegalArgumentException("Node count must be greater than 0");
        }
        if (nodes.length == 1) {
            this.discreteNodes = new float[] {getNormalizedOriginOffest()};
        } else {
            this.discreteNodes = new float[nodes.length];
            float minDistToPivotZ = Float.MAX_VALUE;
            for (int i = 0; i < nodes.length; i ++) {
                float node = nodes[i];
                float normalizedNode = Math.abs(node - rearEndZ) / 16f / getLength();
                float distToPivotZ = Math.abs(node - pivotZ);
                System.out.println("node: " + node + ", normalizedNode: " + normalizedNode + ", distToPivotZ: " + distToPivotZ);
                this.discreteNodes[i] = Mth.clamp(normalizedNode, 0, 1);
                if (distToPivotZ < minDistToPivotZ) {
                    minDistToPivotZ = distToPivotZ;
                }
            }
            System.out.println("discreteNodes: " + Arrays.toString(this.discreteNodes));
            if (minDistToPivotZ >= 1e-3f) {
                throw new IllegalArgumentException("RearEndZ must be within the nodes of the rail");
            }
        }
    }


    @Override
    public void setChildPosition(Unit selected, IWriteableAccessor accessor, float normalizedPos) {
        float snappedPos = snapToNearest(normalizedPos);
        super.setChildPosition(selected, accessor, snappedPos);
    }

    /**
     * 核心机制：获取离输入位置最近的离散节点
     */
    private float snapToNearest(float normalizedPos) {
        normalizedPos = Mth.clamp(normalizedPos, 0, 1);
        if (discreteNodes == null || discreteNodes.length == 0) {
            return normalizedPos;
        }
        float nearest = discreteNodes[0];
        float minDiff = Math.abs(normalizedPos - nearest);
        for (int i = 1; i < discreteNodes.length; i++) {
            float diff = Math.abs(normalizedPos - discreteNodes[i]);
            if (diff < minDiff) {
                minDiff = diff;
                nearest = discreteNodes[i];
            } else {
                break;
            }
        }
        return nearest;
    }

    public float[] getNormalizedDiscreteNodes() {
        return discreteNodes;
    }
}
