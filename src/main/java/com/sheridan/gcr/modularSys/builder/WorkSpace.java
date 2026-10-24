package com.sheridan.gcr.modularSys.builder;


import com.sheridan.gcr.modularSys.IDGenerator;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.slot.ISlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WorkSpace implements IWorkSpace {
    private Node root;
    public static final int IN_TIME_ID_LENGTH = 5;
    private final List<Unit> sequences;
    private final Map<String, Node> idToNode;
    private final Map<Unit, Node> unitToNode;
    private final Map<SlotInstance, List<Node>> slotToNodes;
    private final Map<SlotInstance, Node> slotInstanceBelongsTo;
    private int mutateId = 0;

    public WorkSpace() {
        unitToNode = new HashMap<>();
        sequences = new ArrayList<>();
        idToNode = new HashMap<>();
        slotToNodes = new HashMap<>();
        slotInstanceBelongsTo = new HashMap<>();
    }

    public WorkSpace(@NotNull IModular rootModular) {
        this();
        Objects.requireNonNull(rootModular);
        setRoot(rootModular);
    }

    public Node getRoot() { return root; }


    public String getID(Unit unit) {
        Node node = unitToNode.get(unit);
        return node == null ? Node.EMPTY_ID : node.getID();
    }

    private void setNodeID(Node node, String id) {
        idToNode.put(id, node);
        node.setId(id);
    }

    public Collection<Node> getNodes() {
        return unitToNode.values();
    }

    /**
     * 深度拷贝，用于 commit 和 revert 操作。
     */
    @Override
    public WorkSpace deepCopy() {
        WorkSpace newWorkSpace = new WorkSpace();
        Map<Unit, Unit> oldToNew = new HashMap<>();
        for (Unit originalUnit : this.sequences) {
            Node originalNode = this.unitToNode.get(originalUnit);
            if (originalNode == this.root) {
                newWorkSpace.setRoot(originalUnit.getModule());
                Unit rootUnit = newWorkSpace.getRootUnit();
                rootUnit.copyFrom(originalUnit);
                oldToNew.put(originalUnit, rootUnit);
            } else {
                Node parentNode = originalNode.getParent();
                if (parentNode != null) {
                    Unit newUnit = originalUnit.copy();
                    Unit originalParentUnit = parentNode.getUnit();
                    Unit newParentUnit = oldToNew.get(originalParentUnit);
                    ISlot slot = originalNode.getBelongsToSlot();
                    newWorkSpace.addChild(newParentUnit, slot, newUnit);
                    oldToNew.put(originalUnit, newUnit);
                }
            }
        }
        return newWorkSpace;
    }

    @Override
    public int getMutateId() {
        return mutateId;
    }

    @Override
    public void increaseMutateId() {
        mutateId ++;
    }

    @Override
    public void setOffset(Unit unit, float offset) {
        if (unitToNode.containsKey(unit)) {
            unit.setZOffset(offset);
        }
    }

    @Override
    public String getUnitId(Unit unit) {
        return getID(unit);
    }

    public Node getNode(Unit unit) {
        return unitToNode.get(unit);
    }

    @NotNull
    public List<Node> getNodes(SlotInstance slotInstance) {
        return slotToNodes.getOrDefault(slotInstance, List.of());
    }

    public static String genID(Node node) {
        Node parent = node.getParent();
        if (parent == null) {
            return IDGenerator.genID(node.getModuleID(), IN_TIME_ID_LENGTH);
        } else {
            String parentID = parent.getID();
            SlotInstance belongsTo = node.getBelongsTo();
            int index = parent.getChildren(belongsTo).size() - 1;
            String seed = parentID +  node.getBelongsToSlotName() + node.getModuleID() + "|" + index;
            return IDGenerator.genID(seed, IN_TIME_ID_LENGTH);
        }
    }

    @Override
    public Unit setRoot(IModular modular) {
        if (root != null) {
            return root.getUnit();
        }
        if (modular instanceof ISlotProviderModular) {
            this.root = new Node(modular);
            setNodeID(root, genID(root));
            unitToNode.put(root.getUnit(), root);
            sequences.add(root.getUnit());
            root.getSlots().forEach(slot -> {
                slotInstanceBelongsTo.put(slot, root);
            });
            mutateId++;
            return root.getUnit();
        } else {
            throw new IllegalArgumentException("root module must implement ISlotProviderModular");
        }
    }


    @Override
    public Optional<Unit> addChild(Unit parentUnit, String parentSlotName, String childModuleID) {
        IModular iModular = ModuleRegister.get(childModuleID);
        if (iModular == null) {
            return Optional.empty();
        }
        Unit childUnit = Unit.of(iModular);
        Node node = unitToNode.get(parentUnit);
        if (node != null) {
            SlotInstance bySlot = node.getSlotByName(parentSlotName);
            if (bySlot != null) {
                return addChild(parentUnit, bySlot, childUnit) ?
                        Optional.of(childUnit) :
                        Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Unit> addChild(Unit parent, ISlot parentSlot, Unit child) {
        Node node = unitToNode.get(parent);
        if (node != null) {
            SlotInstance bySlot = node.getBySlot(parentSlot);
            return addChild(parent, bySlot, child) ? Optional.of(child) : Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Unit> addChild(SlotInstance slot, Unit child) {
        Node node = slotInstanceBelongsTo.get(slot);
        if (node != null) {
            return addChild(node.getUnit(), slot, child) ? Optional.of(child) : Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Unit> addChild(SlotInstance slot, IModular modular) {
        return addChild(slot, Unit.of(modular));
    }

    public Node getSlotParent(SlotInstance slotInstance) {
        return slotInstanceBelongsTo.get(slotInstance);
    }

    public boolean addChild(Unit parent, SlotInstance slot, Unit newUnit) {
        Node parentNode = unitToNode.get(parent);
        if (parentNode == null) {
            return false;
        }
        List<Node> children = parentNode.getChildren(slot);
        if (children != null) {
            Node childNode = new Node(newUnit);
            if (parentNode.addChild(slot, childNode)) {
                String newId = genID(childNode);
                childNode.setId(newId);
                unitToNode.put(newUnit, childNode);
                sequences.add(newUnit);
                childNode.getSlots().forEach(s -> slotInstanceBelongsTo.put(s, childNode));
                setNodeID(childNode, newId);
                slotToNodes.put(slot, children);
                mutateId++;
                Builder.callMutatedHook(this, new Accessor(this, true));
                return true;
            }
        }
        return false;
    }

    private void remapID() {
        idToNode.clear();
        for (Unit unit : sequences) {
            Node node = unitToNode.get(unit);
            if(node != null) {
                String newId = genID(node);
                setNodeID(node, newId);
            }
        }
    }

    @Override
    public boolean removeChild(Unit unitToRemove) {
        Node node = unitToNode.get(unitToRemove);
        if (node != null) {
            if (node == root) {
                return false;
            }
            Node parent = node.getParent();
            if (parent != null && !Node.EMPTY_ID.equals(getID(parent.getUnit()))) {
                if (parent.removeChild(node)) {
                    unitToNode.remove(unitToRemove);
                    sequences.remove(unitToRemove);
                    List<SlotInstance> slots = node.getSlots();
                    if (slots != null) {
                        for (SlotInstance slot : slots) {
                            slotInstanceBelongsTo.remove(slot);
                            slotToNodes.remove(slot);
                        }
                    }
                    remapID();
                    mutateId++;
                    Builder.callMutatedHook(this, new Accessor(this, true));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Optional<Pair<Unit, List<Unit>>> replaceChild(Unit oldUnit, Unit newUnit) {
        Node node = unitToNode.get(oldUnit);
        if (node == root) {
            return Optional.empty();
        }
        if (node != null && newUnit != null) {
            Node parent = node.getParent();
            if (parent != null) {
                Node newNode = new Node(newUnit);
                List<Node> removedNodes = parent.replaceChild(node, newNode);
                if (!removedNodes.isEmpty()) {
                    int i = sequences.indexOf(oldUnit);
                    sequences.add(i, newUnit);
                    unitToNode.put(newUnit, newNode);

                    newNode.getSlots().forEach(s -> slotInstanceBelongsTo.put(s, newNode));

                    for (Node removed : removedNodes) {
                        unitToNode.remove(removed.getUnit());
                        sequences.remove(removed.getUnit());
                        List<SlotInstance> slots = removed.getSlots();
                        if (slots != null) {
                            for (SlotInstance slot : slots) {
                                slotToNodes.remove(slot);
                                slotInstanceBelongsTo.remove(slot);
                            }
                        }
                    }

                    slotToNodes.put(newNode.getBelongsTo(), newNode.getParent().getChildren(newNode.getBelongsTo()));

                    remapID();
                    mutateId++;
                    Builder.callMutatedHook(this, new Accessor(this, true));
                    List<Unit> removed = new ArrayList<>();
                    removedNodes.forEach(n -> removed.add(n.getUnit()));
                    return Optional.of(Pair.of(newUnit, removed));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pair<Unit, List<Unit>>> replaceChild(Unit oldUnit, IModular modular) {
        return replaceChild(oldUnit, Unit.of(modular));
    }

    @Override
    public boolean isEmpty() {
        return sequences.isEmpty();
    }

    @Override
    public Unit getRootUnit() {
        return root == null ? null : root.getUnit();
    }

    @Override
    public Optional<Unit> findUnitById(String id) {
        Node node = idToNode.get(id);
        return node == null ? Optional.empty() : Optional.of(node.getUnit());
    }

    @Override
    public List<Unit> getSequencedUnits() {
        return new ArrayList<>(sequences);
    }

    @Override
    public ListTag write() {
        ListTag listTag = new ListTag();
        for (Unit unit : sequences) {
            Node node = unitToNode.get(unit);
            if (node == null) {
                continue;
            }
            CompoundTag compoundTag = new CompoundTag();
            node.write(compoundTag);
            listTag.add(compoundTag);
        }
        return listTag;
    }

    @Override
    public String getId(Unit unit) {
        return getUnitId(unit);
    }

    @Override
    public Optional<Unit> getParentOf(Unit childUnit) {
        Node node = getNode(childUnit);
        if (node == null) {
            return Optional.empty();
        }
        Node parent = node.getParent();
        return parent == null ? Optional.empty() : Optional.of(parent.getUnit());
    }

    @Override
    public List<Unit> getChildrenOf(SlotInstance slotInstance) {
        List<Node> nodes = slotToNodes.get(slotInstance);
        return nodes == null ? List.of() : nodes.stream().map(Node::getUnit).toList();
    }

    @Override
    public ShadowNode getShadowTree() {
        ShadowNode root = null;
        List<ShadowNode> allNodes = null;
        Map<String, ShadowNode> idToNodes = new HashMap<>();
        for (Unit unit : getSequencedUnits()) {
            Node node = getNode(unit);
            if (root == null) {
                root = new ShadowNode(unit, null, node.getID(), true);
                idToNodes.put(node.getID(), root);
                allNodes = root.getAllNodesOfThisTree();
                allNodes.add(root);
            } else {
                String parentID = node.getParentID();
                ShadowNode parent = idToNodes.get(parentID);
                if (parent != null) {
                    ShadowNode child = new ShadowNode(unit, parent, node.getID(), false);
                    parent.addChild(child);
                    allNodes.add(child);
                    idToNodes.put(node.getID(), child);
                }
            }
        }
        if (root != null) {
            root.complete();
        }
        return root;
    }

}
