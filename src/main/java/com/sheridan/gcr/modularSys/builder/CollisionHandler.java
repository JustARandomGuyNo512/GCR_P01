package com.sheridan.gcr.modularSys.builder;


import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.slot.IRail;
import com.sheridan.gcr.modularSys.util.CollisionChecker;
import com.sheridan.gcr.modularSys.util.PivotMap;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;
import java.util.function.BiConsumer;

public class CollisionHandler implements ICollisionHandler{
    private final Map<SlotInstance, Matrix4f> slotPose;
    private final List<Node> voxels;
    private final Set<Node> reversed;
    private WorkSpace workSpace;
    private int lastMutationId;

    public CollisionHandler(WorkSpace workSpace) {
        slotPose = new HashMap<>();
        voxels = new ArrayList<>();
        reversed = new HashSet<>();
        this.workSpace = workSpace;
        lastMutationId = workSpace.getMutateId();
    }

    void setAndUpdate(WorkSpace workSpace) {
        this.workSpace = workSpace;
        lastMutationId = -1;
        update();
    }

    @Override
    public void update() {
        if (lastMutationId == workSpace.getMutateId()) {
            return;
        }
        lastMutationId = workSpace.getMutateId();
        Node root = workSpace.getRoot();
        slotPose.clear();
        voxels.clear();
        reversed.clear();
        if (root == null) {
            return;
        }
        Matrix4f initialPos = new Matrix4f();
        initialPos.translate(0, 0, -7f);
        initialPos.rotate(new Quaternionf().rotateXYZ(0 , (float) Math.toRadians(90f), (float) Math.toRadians(90f)));
        root.dfs(node -> {
            IModular module = node.getModule();
            if (module instanceof IVoxelHandlerModule) {
                voxels.add(node);
            }
            if (module instanceof ISlotProviderModular providerModular) {
                ISlotProvider provider = providerModular.getSlotProvider();
                PivotMap pivotMap = provider.getPivotMap();
                if (pivotMap == null) {
                    return;
                }
                Matrix4f pos;
                SlotInstance parent = node.getBelongsTo();
                if (parent != null) {
                    Matrix4f parentPos = slotPose.get(parent);
                    pos = parentPos == null ? initialPos : parentPos;
                } else {
                    pos = initialPos;
                }
                pos = new Matrix4f(pos);
                pos.translate(0, 0, node.getUnit().getZOffset());
                if (node.shouldReverseModel()) {
                    pos.rotateZ((float) Math.PI);
                    reversed.add(node);
                }
                List<SlotInstance> slots = node.getSlots();
                for (SlotInstance slotInstance : slots) {
                    String slotName = slotInstance.slotName();
                    Matrix4f matrix4f = pivotMap.get(slotName).handleTransform(new Matrix4f(pos));
                    slotPose.put(slotInstance, matrix4f);
                }
            }
        });
    }

    @Override
    public Matrix4f getPose(SlotInstance slotInstance) {
        return slotPose.get(slotInstance);
    }

    @Override
    public Pair<Boolean, Boolean> collisionWithRailBoundary(Unit unit) {
        Node node = workSpace.getNode(unit);
        if (node != null) {
            SlotInstance slotInstance = node.getBelongsTo();
            if (slotInstance == null) {
                return Pair.of(false, false);
            }
            if (slotInstance.getSlot() instanceof IRail rail) {
                Matrix4f pose = getPose(slotInstance);
                if(pose != null) {
                    return rail.collideWithBoundary(unit, pose, new Accessor(workSpace));
                }
            }
        }
        return Pair.of(false, false);
    }

    @NotNull
    @Override
    public List<Unit> collision(Unit with) {
        return collision(with, Collections.emptySet());
    }

    @Override
    public @NotNull List<Unit> collision(Unit with, @NotNull Set<Unit> unCheck) {
        Node thisNode = workSpace.getNode(with);
        if (thisNode == null) {
            return List.of();
        }
        Matrix4f thisPose = getPose(thisNode.getBelongsTo());
        if (thisPose == null) {
            return List.of();
        }
        if (with.getModule() instanceof IVoxelHandlerModule thisVoxel) {
            //避免unit自定义offset污染slot变换
            thisPose = new Matrix4f(thisPose);
            thisPose.translate(0, 0, with.getZOffset());
            if (thisNode.shouldReverseModel() && !reversed.contains(thisNode)) {
                thisPose.rotateZ((float) Math.PI);
            }
            Accessor accessor = new Accessor(workSpace);
            List<Unit> res = new ArrayList<>();
            for (Node other : voxels) {
                if (unCheck.contains(other.getUnit())) {
                    continue;
                }
                IModular module = other.getModule();
                if (module instanceof IVoxelHandlerModule voxelOther) {
                    //boundingBox非mesh精度，允许子模块与父模块重叠
                    if (other == thisNode || other.getParent() == thisNode || thisNode.getParent() == other) {
                        continue;
                    }
                    Matrix4f otherPose = getPose(other.getBelongsTo());
                    if (otherPose != null) {
                        //避免unit自定义offset污染slot变换
                        otherPose = new Matrix4f(otherPose);
                        otherPose.translate(0, 0, other.getUnit().getZOffset());
                        if (other.shouldReverseModel() && !reversed.contains(other)) {
                            otherPose.rotateZ((float) Math.PI);
                        }
                        boolean collided = CollisionChecker.isCollided(
                                thisVoxel.getHandler().getVoxel(with, accessor), thisPose,
                                voxelOther.getHandler().getVoxel(other.getUnit(), accessor), otherPose);
                        if (collided) {
                            res.add(other.getUnit());
                        }
                    }
                }
            }
            return res;
        }
        return List.of();
    }

    @Override
    public void travel(BiConsumer<SlotInstance, Matrix4f> consumer) {
        for (Map.Entry<SlotInstance, Matrix4f> entry : slotPose.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

}
