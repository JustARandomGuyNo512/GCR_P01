package com.sheridan.gcr.modularSys.slot;


import com.google.common.collect.ImmutableSet;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;

import java.util.Set;

public class Slot implements ISlot{
    private final String name;
    private final Set<OperationType> operations;
    private final ImmutableSet<String> tags;
    private boolean defaultHidden;
    private final Direction direction;
    private ISlotFilter filter;

    public Slot(String name,
                Direction direction,
                Set<OperationType> operations,
                Set<String> tags) {
        this(name, direction, operations, tags, false);
    }

    public Slot(String name,
                Direction direction,
                Set<OperationType> operations,
                Set<String> tags, boolean defaultHidden) {
        this.name = name;
        this.direction = direction;
        this.operations = ImmutableSet.copyOf(operations);
        this.tags = ImmutableSet.copyOf(tags);
        this.defaultHidden = defaultHidden;
    }

    protected void setDefaultHidden(boolean defaultHidden) {
        this.defaultHidden = defaultHidden;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean accepts(IModular modular) {
        if (filter == null) {
            return true;
        }
        return filter.test(modular);
    }

    @Override
    public boolean allow(OperationType operationType) {
        return operations.contains(operationType);
    }

    @Override
    public int maxCapacity() {
        return 1;
    }

    @Override
    public ImmutableSet<String> getTags() {
        return tags;
    }

    @Override
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    @Override
    public boolean defaultHidden() {
        return defaultHidden;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public ISlotFilter getFilter() {
        return filter;
    }

    @Override
    public ISlot setFilter(ISlotFilter filter) {
        this.filter = filter;
        return this;
    }

}
