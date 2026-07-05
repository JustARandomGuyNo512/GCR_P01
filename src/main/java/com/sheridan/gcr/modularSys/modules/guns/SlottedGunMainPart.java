package com.sheridan.gcr.modularSys.modules.guns;

import com.google.gson.JsonObject;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.SlotProvider;
import com.sheridan.gcr.modularSys.builder.IBuilder;
import com.sheridan.gcr.modularSys.builder.IWorkSpace;
import com.sheridan.gcr.modularSys.builder.ShadowNode;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import com.sheridan.gcr.modularSys.slot.ISlot;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.modularSys.task.other.SwitchUsingSightTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class SlottedGunMainPart extends Gun implements ISlotProviderModular, ISlottedGun {
    private final ISlotProvider slotProvider;
    private Consumer<IWorkSpace> onModuleTreeInit;

    public SlottedGunMainPart(ResourceLocation id, ResourceLocation pivotMapPath, BaseProperties baseDataModule, DisplayData displayData, RecoilData recoilData, List<IFireMode<?>> fireModes) {
        super(id, baseDataModule, displayData, recoilData, fireModes);
        this.slotProvider = createDefaultSlotProvider(pivotMapPath);
    }

    protected ISlotProvider createDefaultSlotProvider(ResourceLocation pivotMapPath) {
        return new SlotProvider(pivotMapPath);
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {
        super.loadFromJson(jsonObject);
        JsonObject slots = new JsonObject();
        slotProvider.writeToJson(slots);
        jsonObject.add("slots", slots);
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {
        super.writeToJson(jsonObject);
        slotProvider.loadFromJson(jsonObject.getAsJsonObject("slots"));
    }

    @Override
    protected @NotNull CompoundTag getInitialDataTag() {
        return super.getInitialDataTag();
    }


    public SlottedGunMainPart setDefaultModuleInitHandler(Consumer<IWorkSpace> handler) {
        this.onModuleTreeInit = handler;
        return this;
    }

    @Override
    protected void onModuleTreeInit(IWorkSpace workspace, IBuilder builder) {
        super.onModuleTreeInit(workspace, builder);
        if (onModuleTreeInit != null) {
            onModuleTreeInit.accept(workspace);
        }
    }

    @Override
    public SlottedGunMainPart addSlot(ISlot slot) {
        slotProvider.addSlot(slot);
        return this;
    }

    @Override
    public @Nullable IGunTask<?> getTask(ItemStack itemStack, IGunTask.TaskType type, Map<String, Object> args) {
        if (type == IGunTask.TaskType.SWITCH_USING_SIGHT) {
            return new SwitchUsingSightTask(itemStack, this);
        }
        return super.getTask(itemStack, type, args);
    }

    @Override
    public boolean switchUsingSight(ItemStack itemStack) {
        ListTag modulesTag = getModulesTag(itemStack);
        String usingSightID = getUsingSightID(itemStack);
        if ("".equals(usingSightID)) {
            String newId = selectMaxPrioritySight(modulesTag);
            USING_SIGHT.set(newId, rootNodeTag(itemStack));
        } else {
            Map<Integer, List<CompoundTag>> priorityMap = new HashMap<>();
            CompoundTag currentSight = null;
            for (int i = 0; i < modulesTag.size(); i++) {
                CompoundTag data = modulesTag.getCompound(i);
                Unit unit = Unit.of(data);
                if (unit == null) {
                    continue;
                }
                if (unit.getModule() instanceof ISight sight) {
                    int sightPriority = sight.getSightPriority(unit);
                    if (sightPriority != ISight.IGNORE) {
                        priorityMap.computeIfAbsent(sightPriority, k -> new ArrayList<>());
                        priorityMap.get(sightPriority).add(data);
                        String id = data.getString(Unit.IN_TIME_ID);
                        if (id.equals(usingSightID)) {
                            currentSight = data;
                        }
                    }
                }
            }
            if (currentSight == null) {
                return false;
            }
            if (priorityMap.isEmpty()) {
                return false;
            }
            CompoundTag result = chooseEffectiveSight(priorityMap, currentSight);
            String newId = result.getString(Unit.IN_TIME_ID);
            USING_SIGHT.set(newId, rootNodeTag(itemStack));
            return true;
        }
        return true;
    }

    protected CompoundTag chooseEffectiveSight(
            Map<Integer, List<CompoundTag>> sightMap, CompoundTag currentSight) {
        int minPriority = Integer.MAX_VALUE;
        int maxPriority = Integer.MIN_VALUE;
        int currentPriority = -1;
        int currentIndex = -1;
        List<Integer> allPriority = new ArrayList<>();
        for (Map.Entry<Integer, List<CompoundTag>> entry : sightMap.entrySet()) {
            int priority = entry.getKey();
            allPriority.add(priority);
            minPriority = Math.min(minPriority, priority);
            maxPriority = Math.max(maxPriority, priority);
            if (currentIndex != -1 && currentPriority != -1) {
                continue;
            }
            List<CompoundTag> value = entry.getValue();
            int i = value.indexOf(currentSight);
            if (i != -1) {
                currentPriority = priority;
                currentIndex = i;
            }
        }
        allPriority.sort(Comparator.naturalOrder());
        boolean removeGunBaseTier = false;
        if (sightMap.containsKey(ISight.GUN_BASE) && sightMap.size() > 1) {
            if (sightMap.containsKey(ISight.SIDE) && maxPriority > ISight.GUN_BASE) {//GUN_BASE无效
                removeGunBaseTier = true;
            } else if (minPriority >= ISight.GUN_BASE) {
                removeGunBaseTier = true;
            }
        }
        if (removeGunBaseTier) {
            sightMap.remove(ISight.GUN_BASE);
            allPriority.remove(Integer.valueOf(ISight.GUN_BASE));
        }
        List<CompoundTag> compoundTags = sightMap.get(currentPriority);
        if (compoundTags == null) {
            currentPriority = allPriority.getLast();
            compoundTags = sightMap.get(currentPriority);
            return compoundTags.getFirst();
        } else if (currentIndex == compoundTags.size() - 1) {
            int index = allPriority.indexOf(currentPriority);
            currentPriority = allPriority.get((index + 1) % allPriority.size());
            compoundTags = sightMap.get(currentPriority);
            return compoundTags.getFirst();
        } else {
            return compoundTags.get(currentIndex + 1);
        }
    }

    @Override
    public int getAmmoLeft(ItemStack itemStack) {
        return getGunAmmoLeft(itemStack) + getMagAmmoLeft(itemStack);
    }

    @Override
    public void setAmmoLeft(ItemStack itemStack, int ammoLeft) {
        setGunAmmoLeft(itemStack, ammoLeft);
    }

    @Override
    public int getGunAmmoLeft(ItemStack itemStack) {
       return super.getAmmoLeft(itemStack);
    }

    @Override
    public int getMagAmmoLeft(ItemStack itemStack) {
        IAmmoSource magAttachment = getMagAttachment(itemStack);
        if (magAttachment != null) {
            return magAttachment.getAmmoLeft(getAmmoSourceTag(itemStack));
        }
        return 0;
    }

    @Override
    public void setGunAmmoLeft(ItemStack itemStack, int ammoLeft) {
        super.setAmmoLeft(itemStack, ammoLeft);
    }

    @Override
    public void setMagAmmoLeft(ItemStack itemStack, int ammoLeft) {
        IAmmoSource magAttachment = getMagAttachment(itemStack);
        if (magAttachment != null) {
            magAttachment.setAmmoLeft(ammoLeft, getAmmoSourceTag(itemStack));
        }
    }

    @Override
    public @Nullable IAmmoSource getMagAttachment(ItemStack itemStack) {
        String magId = USING_AMMO_SOURCE.get(rootNodeTag(itemStack));
        String thisId = rootNodeId(itemStack);
        if (Objects.equals(magId, thisId)) {
            return null;
        }
        CompoundTag magStates = getNodeStatesTag(itemStack, magId);
        return ModuleRegister.get(IStateModular.MODULE_ID.get(magStates), IAmmoSource.class);
    }

    @Override
    public @Nullable ISight getScopeAttachment(ItemStack itemStack) {
        String scopeId = USING_SIGHT.read(rootNodeTag(itemStack));
        String thisId = rootNodeId(itemStack);
        if (Objects.equals(scopeId, thisId)) {
            return null;
        }
        CompoundTag scopeStates = getNodeStatesTag(itemStack, scopeId);
        return ModuleRegister.get(IStateModular.MODULE_ID.get(scopeStates), ISight.class);
    }

    protected String selectMaxPrioritySight(ListTag modulesTag) {
        String id = "";
        int priority = 0;
        for (int i = 0; i < modulesTag.size(); i++) {
            CompoundTag data = modulesTag.getCompound(i);
            Unit unit = Unit.of(data);
            if (unit == null) {
                continue;
            }
            if (unit.getModule() instanceof ISight sight) {
                int sightPriority = sight.getSightPriority(unit);
                if (sightPriority > priority) {
                    priority = sightPriority;
                    id = unit.getModuleId();
                }
            }
        }
        return id;
    }

    @Override
    protected String findLeftArmHoldID(List<ShadowNode> nodes, StatesUpdateContext context) {
        String id = super.findLeftArmHoldID(nodes, context);
        int maxPriority = this.getPriority(false);
        for (ShadowNode node : nodes) {
            if (node.unit.getModule() instanceof IArmHandlerModular armHandlerModular) {
                int priority = armHandlerModular.getPriority(false);
                if (priority > maxPriority) {
                    id = node.nodeId;
                    maxPriority = priority;
                }
            }
        }
        return id;
    }

    @Override
    protected String findRightArmHoldID(List<ShadowNode> nodes, StatesUpdateContext context) {
        String id = super.findRightArmHoldID(nodes, context);
        int maxPriority = this.getPriority(true);
        for (ShadowNode node : nodes) {
            if (node.unit.getModule() instanceof IArmHandlerModular armHandlerModular) {
                int priority = armHandlerModular.getPriority(true);
                if (priority > maxPriority) {
                    id = node.nodeId;
                    maxPriority = priority;
                }
            }
        }
        return id;
    }

    @Override
    protected String findUsingAmmoSourceID(List<ShadowNode> nodes, StatesUpdateContext context) {
        String id = super.findUsingAmmoSourceID(nodes, context);
        int maxPriority = Integer.MIN_VALUE;
        for (ShadowNode node : nodes) {
            if (node.unit.getModule() instanceof IAmmoSourceView ammoSourceView) {
                int priority = ammoSourceView.getPriority();
                if (priority > maxPriority) {
                    id = node.nodeId;
                    maxPriority = priority;
                }
            }
        }
        return id;
    }
}
