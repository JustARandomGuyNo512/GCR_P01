package com.sheridan.gcr.modularSys.slot;

import com.google.common.collect.ImmutableSet;
import com.sheridan.gcr.modularSys.Direction;

import java.util.Set;

public class ReplaceOnlySlot extends Slot {

    public ReplaceOnlySlot(String name, Direction direction, String... tags) {
        super(name, direction, Set.of(OperationType.ADD, OperationType.REPLACE), ImmutableSet.copyOf(tags));
    }

    public ReplaceOnlySlot(String name, String... tags) {
        this(name, Direction.NONE, tags);
    }

}
