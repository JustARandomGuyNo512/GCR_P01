package com.sheridan.gcr.modularSys.builder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class Accessor implements IWriteableAccessor{
    private final WorkSpace workSpace;
    private boolean writeable;
    private final Set<Unit> hide;

    public Accessor(WorkSpace workSpace) {
        this.workSpace = workSpace;
        hide = new HashSet<>();
    }

    public Accessor(WorkSpace workSpace, boolean writeable) {
        this.workSpace = workSpace;
        hide = new HashSet<>();
        this.writeable = writeable;
    }

    public void hideUnit(Unit unit) {
        Node node = workSpace.getNode(unit);
        if (node != null) {
            node.dfs(n -> {
                Unit unit1 = n.getUnit();
                hide.add(unit1);
            });
        }
    }

    public void setHideUnits(Set<Unit> units) {
        hide.clear();
        for (Unit unit : units) {
            hideUnit(unit);
        }
    }

    Set<Unit> getHide() {
        return hide;
    }

    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    @Override
    public void writeCustomParam(Unit unit, String key, int value) {
        if (writeable) {
            unit.setRenderParams(key, value);
        }
    }

    @Override
    public boolean setSlotHide(SlotInstance slot, boolean hide) {
        if (!writeable) {
            return false;
        }
        List<Node> node = workSpace.getNodes(slot);
        if (node.isEmpty()) {
            slot.setHidden(hide);
            return true;
        }
        return false;
    }

    @Override
    public void setOffset(Unit unit, float offset) {
        if (writeable) {
            unit.setZOffset(offset);
        }
    }

    public boolean isEmpty() {
        return workSpace.isEmpty();
    }

    public boolean invisible(Unit unit) {
        return hide.contains(unit);
    }

    @Override
    public Optional<Unit> getParent(Unit childUnit) {
        if (invisible(childUnit)) {
            return Optional.empty();
        }
        Node node = workSpace.getNode(childUnit);
        if (node != null) {
            Node parent = node.getParent();
            if (parent == null || invisible(parent.getUnit())) {
                return Optional.empty();
            }
            return Optional.of(parent.getUnit());
        }
        return Optional.empty();
    }

    @Override
    public List<SlotInstance> getSlots(Unit parent) {
        if (invisible(parent)) {
            return List.of();
        }
        Node node = workSpace.getNode(parent);
        if (node != null) {
            return node.getSlots();
        }
        return List.of();
    }

    @Override
    public Optional<SlotInstance> getSlot(Unit parent, String slotName) {
        if (invisible(parent)) {
            return Optional.empty();
        }
        Node node = workSpace.getNode(parent);
        if (node != null) {
            SlotInstance slotByName = node.getSlotByName(slotName);
            return slotByName == null ? Optional.empty() : Optional.of(slotByName);
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<SlotInstance>> filterSlots(Unit parent, Predicate<SlotInstance> predicate) {
        List<SlotInstance> slots = getSlots(parent);
        List<SlotInstance> list = slots.stream().filter(predicate).toList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list);
    }

    @Override
    public Optional<List<Unit>> filter(Predicate<Unit> predicate) {
        List<Unit> res = new ArrayList<>();
        for (Unit unit : workSpace.getSequencedUnits()) {
            if (invisible(unit)) {
                continue;
            }
            if (predicate.test(unit)) {
                res.add(unit);
            }
        }
        return res.isEmpty() ? Optional.empty() : Optional.of(res);
    }

    @Override
    public Optional<Unit> first(Predicate<Unit> predicate) {
        for (Unit unit : workSpace.getSequencedUnits()) {
            if (invisible(unit)) {
                continue;
            }
            if (predicate.test(unit)) {
                return Optional.of(unit);
            }
        }
        return Optional.empty();
    }

    @Override
    public Unit root() {
        return workSpace.getRootUnit();
    }

    @Override
    public List<Unit> getSequencedUnits() {
        List<Unit> res = new ArrayList<>();
        for (Unit unit : workSpace.getSequencedUnits()) {
            if (invisible(unit)) {
                continue;
            }
            res.add(unit);
        }
        return res;
    }

    @Override
    public Optional<SlotInstance> getBelongsTo(Unit unit) {
        Node node = workSpace.getNode(unit);
        if (node != null) {
            SlotInstance belongsTo = node.getBelongsTo();
            return belongsTo == null ? Optional.empty() : Optional.of(belongsTo);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Unit> getSlotParent(SlotInstance slotInstance) {
        Node slotParent = workSpace.getSlotParent(slotInstance);
        return slotParent == null ? Optional.empty() : Optional.of(slotParent.getUnit());
    }

    @Override
    public boolean contains(Unit unit) {
        if (invisible(unit)) {
            return false;
        }
        return workSpace.getNode(unit) != null;
    }

    @Override
    public boolean slotEmpty(SlotInstance slotInstance) {
        return workSpace.getChildrenOf(slotInstance).isEmpty();
    }

    @Override
    public boolean slotEmpty(Unit parent, String slotName) {
        Node node = workSpace.getNode(parent);
        if (node == null) {
            return true;
        }
        SlotInstance slotByName = node.getSlotByName(slotName);
        return slotByName == null || slotEmpty(slotByName);
    }

    @Override
    public List<Unit> getSlotChildren(Unit parent, String slotName) {
        AtomicReference<List<Unit>> res = new AtomicReference<>(List.of());
        getSlot(parent, slotName).ifPresent(slotInstance ->
                res.set(workSpace.getChildrenOf(slotInstance)));
        return res.get();
    }

    @Override
    public List<Unit> getSlotChildren(SlotInstance slotInstance) {
        return workSpace.getChildrenOf(slotInstance);
    }
}
