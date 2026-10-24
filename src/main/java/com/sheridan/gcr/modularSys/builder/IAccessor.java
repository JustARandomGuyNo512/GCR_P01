package com.sheridan.gcr.modularSys.builder;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface IAccessor {

    Optional<Unit> getParent(Unit childUnit);

    List<SlotInstance> getSlots(Unit parent);

    Optional<SlotInstance> getSlot(Unit parent, String slotName);
    Optional<List<SlotInstance>> filterSlots(Unit parent, Predicate<SlotInstance> predicate);

    Optional<List<Unit>> filter(Predicate<Unit> predicate);

    Optional<Unit> first(Predicate<Unit> predicate);

    Unit root();

    List<Unit> getSequencedUnits();

    Optional<SlotInstance> getBelongsTo(Unit unit);

    Optional<Unit> getSlotParent(SlotInstance slotInstance);

    boolean contains(Unit unit);
    boolean slotEmpty(SlotInstance slotInstance);
    boolean slotEmpty(Unit parent, String slotName);

    List<Unit> getSlotChildren(Unit parent, String slotName);
    List<Unit> getSlotChildren(SlotInstance slotInstance);
}