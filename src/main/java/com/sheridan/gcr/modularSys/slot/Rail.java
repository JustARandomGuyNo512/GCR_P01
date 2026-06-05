package com.sheridan.gcr.modularSys.slot;

import com.google.common.collect.ImmutableSet;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.util.CollisionChecker;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import java.util.Set;

public class Rail extends Slot implements IRail {
    private final float length;
    private final float originOffset;

    public Rail(String name, Direction direction, float rearEndZ, float pivotZ, float farEndZ, String... tags) {
        super(name, direction, Set.of(
                OperationType.ADD,
                OperationType.REMOVE,
                OperationType.REPLACE
        ), ImmutableSet.copyOf(tags));
        if (rearEndZ <= farEndZ) {
            throw new IllegalArgumentException("rearEndZ must be greater than farEndZ");
        }
        if (pivotZ <= rearEndZ && pivotZ >= farEndZ) {
            this.length = (rearEndZ - farEndZ) / 16f;
            this.originOffset = (Math.abs(pivotZ - rearEndZ) / 16f) / this.length;
        } else {
            throw new IllegalArgumentException("pivotZ must be between farEndZ and rearEndZ");
        }
    }

    @Override
    public Pair<Boolean, Boolean> collideWithBoundary(Unit child, Matrix4f slotPose, Accessor accessor) {
        if (child.getModule() instanceof IVoxelHandlerModule voxelModule) {
            IVoxelHandler handler = voxelModule.getHandler();
            if (handler == null) {
                return Pair.of(false, false);
            }
            Pair<Boolean, Boolean> ignore = handler.ignoreBoundaryCollision();
            Matrix4f childPos = new Matrix4f(slotPose);
            childPos.translate(0, 0, child.getZOffset());
            float rearEndZ = originOffset * length;
            float farEndZ = - (1 - originOffset) * length;
            Matrix4f rearPos = new Matrix4f(slotPose);
            rearPos.translate(0, 0, rearEndZ);
            Matrix4f farPos = new Matrix4f(slotPose);
            farPos.translate(0, 0, farEndZ);
            boolean collidedRear = !ignore.getLeft() &&
                    CollisionChecker.isCollided(handler.getVoxel(child, accessor), childPos, SlotBoundaryVoxels.getRearVoxel(), rearPos);
            boolean collidedFar = !ignore.getRight() &&
                    CollisionChecker.isCollided(handler.getVoxel(child, accessor), childPos, SlotBoundaryVoxels.getFarVoxel(), farPos);
            return Pair.of(collidedRear, collidedFar);
        }
        return Pair.of(false, false);
    }

    @Override
    public void setChildPosition(Unit selected, IWriteableAccessor accessor, float pos) {
        pos = Mth.clamp(pos, 0, 1);
        float dist = (pos - originOffset) * length;
        accessor.setOffset(selected, -dist);
    }

    @Override
    public float getLength() {
        return length;
    }

    @Override
    public float getNormalizedOriginOffest() {
        return originOffset;
    }

    @Override
    public float getNormalizedChildPosition(Unit unit) {
        float z = unit.getZOffset();
        return Mth.clamp((originOffset - (z / length)) , 0, 1);
    }

    @Override
    public int maxCapacity() {
        return 16;
    }
}
