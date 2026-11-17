package com.sheridan.gcr.modularSys.modules.guns;

import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.Builder;
import com.sheridan.gcr.modularSys.builder.IBuilder;
import com.sheridan.gcr.modularSys.builder.IReadOnlyTree;
import com.sheridan.gcr.modularSys.builder.IWorkSpace;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.builder.ShadowNode;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.builder.WorkSpace;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.slot.ISlot;
import com.sheridan.gcr.network.s2c.CommitModuleTreeResponsePacket;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;


import com.google.gson.JsonObject;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.SlotProvider;
import com.sheridan.gcr.modularSys.builder.*;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class SlottedGunMainPart extends Gun implements ISlotProviderModular, ISlottedGun {
    private final ISlotProvider slotProvider;
    private BiConsumer<IWorkSpace, IWriteableAccessor> onModuleTreeInit;

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

    public SlottedGunMainPart setDefaultModuleInitHandler(BiConsumer<IWorkSpace, IWriteableAccessor> handler) {
        this.onModuleTreeInit = handler;
        return this;
    }

    @Override
    protected void onModuleTreeInit(IWorkSpace workspace, IBuilder builder) {
        super.onModuleTreeInit(workspace, builder);
        if (onModuleTreeInit != null) {
            IWriteableAccessor writeableAccessor = builder.getWriteableAccessor();
            onModuleTreeInit.accept(workspace, writeableAccessor);
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
        if (type == IGunTask.TaskType.RELOAD) {
            if (!shouldReload(itemStack)) {
                return null;
            }
            return getReloadTask(itemStack);//new ARReloadTask(itemStack, this);
        }
        if (type == IGunTask.TaskType.REMOVE_STUCK && isStuck(itemStack)) {
            return getRemoveStuckTask(itemStack);//new ARRemoveStuckTask(itemStack, this);
        }
        return super.getTask(itemStack, type, args);
    }

    protected abstract IGunTask<?> getReloadTask(ItemStack itemStack);

    protected abstract IGunTask<?> getRemoveStuckTask(ItemStack itemStack);

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

    protected boolean shouldReload(ItemStack itemStack) {
        IAmmoSource magAttachment = getMagAttachment(itemStack);
        if (magAttachment == null) {
            return getGunAmmoLeft(itemStack) < 1;
        } else {
            int ammoLeft = getAmmoLeft(itemStack);
            return ammoLeft < magAttachment.getMaxCapacity() + 1;
        }
    }

    public static boolean isMagCompatible(IGun gun, IModular mag) {
        if (gun instanceof ISlotProviderModular provider) {
            for (ISlot slot : provider.getSlotProvider().getSlots()) {
                if ("MAG".equalsIgnoreCase(slot.getName()) && slot.accepts(mag)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int findBestCompatibleMagazineSlot(Player player, IGun gun) {
        int bestSlot = -1;
        int maxAmmo = -1;
        NonNullList<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.getItem() instanceof MagItem magItem) {
                if (isMagCompatible(gun, magItem.getModule())) {
                    int ammo = MagItem.getAmmoLeft(stack);
                    if (ammo > maxAmmo) {
                        maxAmmo = ammo;
                        bestSlot = i;
                    }
                }
            }
        }
        return bestSlot;
    }

    @Override
    public void reload(ItemStack itemStack, Player player, int targetSlot) {
        swapMagazine(itemStack, player, targetSlot);
    }

    @Override
    public void reload(ItemStack itemStack, Player player) {
        reload(itemStack, player, -1);
    }

        public boolean swapMagazine(ItemStack itemStack, Player player, int targetSlot) {
        String usingAmmoSourceId = USING_AMMO_SOURCE.get(rootNodeTag(itemStack));
        String thisId = rootNodeId(itemStack);
        boolean hasExistingMag = !Objects.equals(usingAmmoSourceId, thisId);

        ListTag originalModules = getModulesTag(itemStack);
        String oldMagModuleId = null;
        int oldMagAmmoLeft = 0;

        for (int i = 0; i < originalModules.size(); i++) {
            CompoundTag nodeTag = originalModules.getCompound(i);
            if ("MAG".equalsIgnoreCase(nodeTag.getString(Unit.SLOT_NAME))) {
                oldMagModuleId = nodeTag.getString(Unit.MODULE_ID);
                break;
            }
        }

        if (hasExistingMag) {
            CompoundTag magStates = getNodeStatesTag(itemStack, usingAmmoSourceId);
            if (magStates != null) {
                oldMagAmmoLeft = IAmmoSource.AMMO_LEFT.get(magStates);
            }
        }

        // 1. UNLOAD ONLY (Shift + R)
        if (targetSlot == -2) {
            if (!hasExistingMag || oldMagModuleId == null) {
                return false;
            }
            try {
                IBuilder builder = new Builder();
                builder.initAndClear(originalModules);
                WorkSpace workspace = (WorkSpace) builder.getWorkspace();

                Unit oldUnit = null;
                for (Unit unit : workspace.getSequencedUnits()) {
                    Node node = workspace.getNode(unit);
                    if (node != null && "MAG".equalsIgnoreCase(node.getBelongsToSlotName())) {
                        oldUnit = unit;
                        break;
                    }
                }
                if (oldUnit != null) {
                    workspace.removeChild(oldUnit);
                }
                builder.commit();
                IReadOnlyTree warehouse = builder.getWarehouse();
                ListTag finalModules = warehouse.write();
                setModulesTag(itemStack, finalModules);

                CompoundTag states = getStatesTag(itemStack);
                ShadowNode shadowTree = warehouse.getShadowTree();
                StatesUpdateContext statesUpdateContext = new StatesUpdateContext(this, shadowTree, states);
                statesUpdateContext.autoExec();
                setStatesTag(itemStack, states);

                CompoundTag properties = reCalculateProperties(warehouse);
                setPropertiesTag(itemStack, properties);

                // Return old magazine to player
                IModular oldModular = ModuleRegister.get(oldMagModuleId);
                if (oldModular != null && oldModular.getBindItem() != null) {
                    ItemStack oldMagItem = new ItemStack(oldModular.getBindItem());
                    MagItem.setAmmoLeft(oldMagItem, oldMagAmmoLeft);
                    if (!player.getInventory().add(oldMagItem)) {
                        player.drop(oldMagItem, false);
                    }
                }

                notifyDataChanged(itemStack);
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new CommitModuleTreeResponsePacket(
                            checkAndGetRaw(itemStack), "", true));
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        // 2. NORMAL RELOAD / SWAP
        ItemStack newMagStack = ItemStack.EMPTY;
        MagItem newMagItem = null;
        int newAmmo = 0;

        if (targetSlot >= 0 && targetSlot < player.getInventory().items.size()) {
            ItemStack candidate = player.getInventory().items.get(targetSlot);
            if (candidate.getItem() instanceof MagItem mag && isMagCompatible(this, mag.getModule())) {
                newMagStack = candidate;
                newMagItem = mag;
                newAmmo = MagItem.getAmmoLeft(candidate);
            }
        }

        if (newMagItem == null) {
            int autoSlot = findBestCompatibleMagazineSlot(player, this);
            if (autoSlot >= 0) {
                newMagStack = player.getInventory().items.get(autoSlot);
                newMagItem = (MagItem) newMagStack.getItem();
                newAmmo = MagItem.getAmmoLeft(newMagStack);
            }
        }

        if (newMagItem == null) {
            return false;
        }

        try {
            IBuilder builder = new Builder();
            builder.initAndClear(originalModules);
            WorkSpace workspace = (WorkSpace) builder.getWorkspace();
            Unit rootUnit = workspace.getRootUnit();

            Unit oldUnit = null;
            for (Unit unit : workspace.getSequencedUnits()) {
                Node node = workspace.getNode(unit);
                if (node != null && "MAG".equalsIgnoreCase(node.getBelongsToSlotName())) {
                    oldUnit = unit;
                    break;
                }
            }

            // Remove old mag if present
            if (oldUnit != null) {
                workspace.removeChild(oldUnit);
            }

            // Add new magazine
            var addRes = workspace.addChild(rootUnit, "MAG", newMagItem.getModule().getID());
            if (addRes.isEmpty()) {
                return false;
            }

            newMagStack.shrink(1);

            builder.commit();
            IReadOnlyTree warehouse = builder.getWarehouse();
            ListTag finalModules = warehouse.write();
            setModulesTag(itemStack, finalModules);

            // Update states
            CompoundTag states = getStatesTag(itemStack);
            ShadowNode shadowTree = warehouse.getShadowTree();
            StatesUpdateContext statesUpdateContext = new StatesUpdateContext(this, shadowTree, states);
            statesUpdateContext.autoExec();

            // Update ammo in chamber and magazine
            String newMagNodeId = USING_AMMO_SOURCE.get(states.getCompound(rootNodeId(itemStack)));
            CompoundTag newMagStates = states.getCompound(newMagNodeId);
            if (newMagStates != null) {
                if (getGunAmmoLeft(itemStack) <= 0 && newAmmo > 0) {
                    setGunAmmoLeft(itemStack, 1);
                    newAmmo -= 1;
                }
                IAmmoSource.AMMO_LEFT.set(newAmmo, newMagStates);
            }
            if (this instanceof AR ar) {
                ar.setBoltLocked(false, states.getCompound(rootNodeId(itemStack)));
            }
            setStatesTag(itemStack, states);

            // Recalculate properties
            CompoundTag properties = reCalculateProperties(warehouse);
            setPropertiesTag(itemStack, properties);

            // Give old magazine back ONLY if there was an old magazine
            if (oldUnit != null && oldMagModuleId != null) {
                IModular oldModular = ModuleRegister.get(oldMagModuleId);
                if (oldModular != null && oldModular.getBindItem() != null) {
                    ItemStack oldMagItem = new ItemStack(oldModular.getBindItem());
                    MagItem.setAmmoLeft(oldMagItem, oldMagAmmoLeft);
                    if (!player.getInventory().add(oldMagItem)) {
                        player.drop(oldMagItem, false);
                    }
                }
            }

            notifyDataChanged(itemStack);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new CommitModuleTreeResponsePacket(
                        checkAndGetRaw(itemStack), "", true));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
