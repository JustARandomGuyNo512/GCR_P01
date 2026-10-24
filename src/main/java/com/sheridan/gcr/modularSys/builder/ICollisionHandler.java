package com.sheridan.gcr.modularSys.builder;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public interface ICollisionHandler {
    void update();

    Matrix4f getPose(SlotInstance slotInstance);

    Pair<Boolean, Boolean> collisionWithRailBoundary(Unit unit);

    @NotNull
    List<Unit> collision(Unit with);

    @NotNull
    List<Unit> collision(Unit with, @NotNull Set<Unit> unCheck);

    void travel(BiConsumer<SlotInstance, Matrix4f> consumer);
}
