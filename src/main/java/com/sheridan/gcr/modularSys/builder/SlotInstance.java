package com.sheridan.gcr.modularSys.builder;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.slot.ISlot;
import com.sheridan.gcr.modularSys.slot.OperationType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SlotInstance {
    private final ISlot slot;
    private boolean isHidden;

    public SlotInstance(@NotNull ISlot slot) {
        Objects.requireNonNull(slot);
        this.slot = slot;
        isHidden = slot.defaultHidden();
    }

    @NotNull
    public ISlot getSlot() {
        return slot;
    }

    public boolean isHidden() {
        return isHidden;
    }

    void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    void flush() {
        isHidden = slot.defaultHidden();
    }

    public String slotName() {
        return slot.getName();
    }

    public boolean allow(OperationType type) {
        return !isHidden && slot.allow(type);
    }

    public boolean accepts(IModular modular) {
        return modular != null && !isHidden && slot.accepts(modular);
    }

    public Direction getDirection() {
        return slot.getDirection();
    }
}
