package com.sheridan.gcr.modularSys.slot;

import com.sheridan.gcr.modularSys.builder.Accessor;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public interface IRail {
    float getLength();

    float getNormalizedOriginOffest();

    float getNormalizedChildPosition(Unit instance);

    Pair<Boolean, Boolean> collideWithBoundary(Unit child, Matrix4f slotPose, Accessor accessor);

    void setChildPosition(Unit selectedNode, IWriteableAccessor accessor, float normalizedPos);
}
