package com.sheridan.gcr.modularSys.builder;

import com.sheridan.gcr.modularSys.*;
import com.sheridan.gcr.modularSys.slot.ISlot;
import com.sheridan.gcr.modularSys.slot.OperationType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class Node {
    public static final String EMPTY_ID = "EMPTY";
    private String id = EMPTY_ID;
    private int depth = 0;
    private final Unit unit;
    private Node parent;
    private final Map<SlotInstance, List<Node>> slotToChildren;
    private final Map<ISlot, SlotInstance> slotToInstance;
    private final Map<String, SlotInstance> slotNameToInstance;
    private SlotInstance belongsTo;

    public Node(@NotNull IModular modular) {
        this(Unit.of(modular));
    }

    public Node(@NotNull Unit unit) {
        Objects.requireNonNull(unit);
        this.unit = unit;
        this.slotToChildren = new LinkedHashMap<>();
        this.slotToInstance = new HashMap<>();
        this.slotNameToInstance = new HashMap<>();
        if (unit.getModule() instanceof ISlotProviderModular modular) {
            ISlotProvider slotProvider = modular.getSlotProvider();
            for (ISlot slot : slotProvider.getSlots()) {
                SlotInstance slotInstance = new SlotInstance(slot);
                slotToChildren.put(slotInstance, new ArrayList<>());
                slotToInstance.put(slot, slotInstance);
                slotNameToInstance.put(slot.getName(), slotInstance);
            }
        }
    }

    // 提供一系列方便的结构操作方法
    public String getID() {
        return id;
    }

    public void setId(String newId) {
        this.id = newId;
    }

    public Unit getUnit() {
        return unit;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public String getParentID() {
        return parent == null ? EMPTY_ID : parent.getID();
    }

    public List<Node> getChildren(SlotInstance slot) {
        return slotToChildren.get(slot);
    }
    public Map<SlotInstance, List<Node>> getAllChildren() {
        return slotToChildren;
    }

    public IModular getModule() {
        return unit == null ? null : unit.getModule();
    }

    public SlotInstance getBelongsTo() {
        return belongsTo;
    }

    public ISlot getBelongsToSlot() {
        return belongsTo == null ? null : belongsTo.getSlot();
    }

    public String getBelongsToSlotName() {
        return belongsTo == null ? Unit.NONE : belongsTo.getSlot().getName();
    }

    public void setBelongsTo(SlotInstance belongsTo) {
        this.belongsTo = belongsTo;
    }

    public SlotInstance getBySlot(ISlot slot) {
        return slotToInstance.get(slot);
    }

    public SlotInstance getSlotByName(String name) {
        return slotNameToInstance.get(name);
    }

    public boolean removeFromParent() {
        if (parent != null && belongsTo != null) {
            return parent.removeChild(this);
        }
        return false;
    }

    public String getModuleID() {
        return unit.getModule().getID();
    }

    public boolean addChild(ISlot slot, Node child) {
        SlotInstance instance = this.getBySlot(slot);
        if (instance != null) {
            return this.addChild(instance, child);
        }
        return false;
    }

    public boolean hasChild(String slotName) {
        SlotInstance slotInstance = slotNameToInstance.get(slotName);
        if (slotInstance != null) {
            return slotToChildren.containsKey(slotInstance) && !slotToChildren.get(slotInstance).isEmpty();
        }
        return false;
    }

    public boolean addChild(SlotInstance slot, Node child) {
        if (slot == null || child == null) {
            return false;
        }
        if (slotToChildren.containsKey(slot) && slot.accepts(child.unit.getModule())) {
            List<Node> nodes = slotToChildren.get(slot);
            if (nodes.size() < slot.getSlot().maxCapacity()) {
                nodes.add(child);
                child.setParent(this);
                child.setBelongsTo(slot);
                child.depth = this.depth + 1;
                return true;
            }
        }
        return false;
    }

    public List<Node> replaceChild(Node oldNode, Node newNode) {
        if (oldNode == null || newNode == null || oldNode.getBelongsTo() == null) {
            return List.of();
        }
        SlotInstance oldBelongsTo = oldNode.getBelongsTo();
        if (oldBelongsTo == null || !slotToChildren.containsKey(oldBelongsTo)) {
            return List.of();
        }
        if (!oldBelongsTo.allow(OperationType.REPLACE) || !oldBelongsTo.accepts(newNode.getModule())) {
            return List.of();
        }
        List<Node> children = slotToChildren.get(oldBelongsTo);
        int idx = children.indexOf(oldNode);
        if (idx >= 0) {
            children.set(idx, newNode);
            newNode.setParent(this);
            newNode.setBelongsTo(oldBelongsTo);
            newNode.depth = this.depth + 1;
            newNode.refineDepth();
            List<Node> removed = new ArrayList<>();
            removed.add(oldNode);

            // 迁移 oldNode 的子节点
            List<Map.Entry<SlotInstance, List<Node>>> snapshot = new ArrayList<>(oldNode.slotToChildren.entrySet());
            for (Map.Entry<SlotInstance, List<Node>> entry : snapshot) {
                ISlot slot = entry.getKey().getSlot();
                List<Node> oldChildren = new ArrayList<>(entry.getValue());
                for (Node child : oldChildren) {
                    if (child.removeFromParent() && !newNode.addChild(slot, child)) {
                        removed.add(child);
                    }
                }
            }
            return removed;
        }
        return List.of();
    }

    public void refineDepth() {
        this.dfs(node -> node.depth = node.getParent().depth + 1);
    }

    public void dfs(Consumer<Node> visitor) {
        visitor.accept(this);
        for (List<Node> children : slotToChildren.values()) {
            for (Node child : children) {
                child.dfs(visitor);
            }
        }
    }

    public void print(int tab) {
        String prefix = "   ".repeat(tab);
        System.out.println(prefix + "{module: " + unit.getModule().getID());
        System.out.println(prefix + " obj: " + System.identityHashCode(this));
        System.out.println(prefix + " slot: " + getBelongsToSlotName());
        System.out.println(prefix + " id: " + getID());
        System.out.println(prefix + " parent id: " + getParentID() + "}");
        for (Map.Entry<SlotInstance, List<Node>> entry : slotToChildren.entrySet()) {
            for (Node child : entry.getValue()) {
                child.print(tab + 1);
            }
        }
    }

    public List<SlotInstance> getSlots() {
        return new ArrayList<>(slotToInstance.values());
    }

    public boolean removeChild(Node child) {
        if (child == null || !child.belongsTo.allow(OperationType.REMOVE)) {
            return false;
        }
        if (child.parent == this) {
            return slotToChildren.get(child.belongsTo).remove(child);
        }
        return false;
    }

    public void write(CompoundTag compoundTag) {
        compoundTag.putString(Unit.IN_TIME_ID, id);
        compoundTag.putString(Unit.PARENT_ID, getParentID());
        compoundTag.putString(Unit.SLOT_NAME, getBelongsToSlotName());
        compoundTag.putBoolean(Unit.REVERSE_MODEL, shouldReverseModel());
        unit.write(compoundTag);
    }

    public boolean shouldReverseModel() {
        if (belongsTo != null) {
            Direction slotDirection = belongsTo.getDirection();
            Direction unitDirection = unit.getDirection();
            if (slotDirection == Direction.NONE || unitDirection == Direction.NONE) {
                return false;
            }
            return slotDirection != unitDirection;
        } else {
            return false;
        }
    }

    public boolean isFixedPosition() {
        return unit.getModule().fixedPosition();
    }

    public Map<String, Integer> getCustomRenderParams() {
        return unit.getCustomParams();
    }

    @Nullable
    public static Node read(ListTag data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        Node root = null;
        Map<String, Node> idToNodes = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);
            String parentId = tag.getString(Unit.PARENT_ID);
            String id = tag.getString(Unit.IN_TIME_ID);
            String moduleId = tag.getString(Unit.MODULE_ID);
            IModular modular = ModuleRegister.get(moduleId);
            if (modular == null) {
                continue;
            }
            Node node = new Node(modular);
            node.id = id;
            boolean loadParam = false;
            if (EMPTY_ID.equals(parentId) && root == null) { // root
                root = node;
                idToNodes.put(id, node);
                loadParam = true;
            } else {
                Node parent = idToNodes.get(parentId);
                if (parent != null) {
                    String slotName = tag.getString(Unit.SLOT_NAME);
                    SlotInstance slot = parent.getSlotByName(slotName);
                    if (slot != null) {
                        parent.addChild(slot, node);
                        idToNodes.put(id, node);
                        loadParam = true;
                    }
                }
            }
            if (loadParam) {
                node.unit.read(tag);
            }
        }
        return root;
    }

    static boolean diff(Node nodeA, Node nodeB) {
        if (nodeA == nodeB) {
            return false;
        }
        if (nodeA == null || nodeB == null) {
            return true;
        }
        if (nodeA.getUnit().diff(nodeB.getUnit())) {
            return true;
        }
        SlotInstance belongsToA = nodeA.getBelongsTo();
        SlotInstance belongsToB = nodeB.getBelongsTo();
        if (belongsToA != belongsToB) {
            if (belongsToA == null || belongsToB == null) {
                return true;
            }
            if (belongsToA.isHidden() != belongsToB.isHidden()) {
                return true;
            }
            ISlot slotA = belongsToA.getSlot();
            ISlot slotB = belongsToB.getSlot();
            return slotA != slotB;
        }
        return false;
    }

    public int getDepth() {
        return depth;
    }
}