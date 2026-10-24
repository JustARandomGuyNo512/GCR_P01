package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.network.s2c.CommitModuleTreeResponsePacket;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.sheridan.gcr.network.GCRNetwork;
import com.sheridan.gcr.network.PacketContext;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class CommitModuleTreePacket implements IPacket<CommitModuleTreePacket> {
    public static final ResourceLocation ID = GCR.RL("set_module_tree");
    public ListTag data;

    public CommitModuleTreePacket(ListTag data) {
        this.data = data;
    }

    public static CommitModuleTreePacket decode(FriendlyByteBuf buf) {
        CompoundTag wrapper = buf.readNbt();
        ListTag tagData = wrapper == null ? new ListTag() : wrapper.getList("modules", 10);
        return new CommitModuleTreePacket(tagData);
    }

    public void encode(FriendlyByteBuf buf) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("modules", data);
        buf.writeNbt(wrapper);
    }


    @Override
    public void onClient(CommitModuleTreePacket packet, PacketContext context) {

    }

    @Override
    public void onServer(CommitModuleTreePacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack mainHandItem = player.getMainHandItem();
            if (packet != null && mainHandItem.getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                CompoundTag originalTag = gunItem.getGun().checkAndGetRaw(mainHandItem);
                ListTag originalModules = gunItem.getGun().getModulesTag(mainHandItem);
                try {
                    IBuilder builder = new Builder();
                    builder.initAndClear(packet.data);
                    List<ValidateResult> commit = builder.commit();
                    boolean hasError = false;
                    for (ValidateResult result : commit) {
                        if (!result.isCommitAllowed()) {
                            hasError = true;
                            break;
                        }
                    }
                    String msg = hasError ? Component.translatable("packet.set_module_tree.fixed").getString() : "";
                    IReadOnlyTree warehouse = builder.getWarehouse();
                    ListTag finalModules = warehouse.write();

                    // MAG stores physical ammunition; never exchange it via the
                    // generic attachment editor or ammo/NBT can be duplicated.
                    if (!magazineNodes(originalModules).equals(magazineNodes(finalModules))) {
                        GCRNetwork.sendToPlayer((ServerPlayer) player,
                                new CommitModuleTreeResponsePacket(originalTag,
                                        "Use R to insert or remove a magazine.", false));
                        return;
                    }

                    Pair<List<CompoundTag>, List<CompoundTag>> diff = Builder.diffByModule(originalModules, finalModules);
                    List<CompoundTag> inc = diff.getLeft();//增加的
                    List<CompoundTag> dec = diff.getRight();//减少的

                    Map<ModuleItem<?>, Integer> preTakeoff = new HashMap<>();
                    for (CompoundTag tag : dec) {
                        IModular module = ModuleRegister.get(tag.getString("module"));
                        if (module != null && module.getBindItem() != null) {
                            preTakeoff.put(module.getBindItem(), preTakeoff.getOrDefault(module.getBindItem(), 0) + 1);
                        }
                    }
                    NonNullList<ItemStack> items = player.getInventory().items;
                    Set<Integer> removeSlotIndex = new HashSet<>();
                    for (CompoundTag newModule : inc) {
                        String moduleId = newModule.getString("module");
                        IModular module = ModuleRegister.get(moduleId);
                        if (module == null) {
                            msg = Component.translatable("packet.set_module_tree.module_not_exist").getString().replace("$id", moduleId);
                            GCRNetwork.sendToPlayer((ServerPlayer) player, new CommitModuleTreeResponsePacket(originalTag, msg, false));
                            return;
                        }
                        boolean found = false;
                        Integer left = preTakeoff.get(module.getBindItem());
                        if (left != null && left > 0) {
                            preTakeoff.put(module.getBindItem(), left - 1);
                            found = true;
                        } else {
                            for (int i = 0; i < items.size(); i++) {
                                ItemStack itemStack = items.get(i);
                                if (!removeSlotIndex.contains(i) &&
                                        itemStack.getItem() instanceof ModuleItem<?> moduleItem &&
                                        Objects.equals(moduleItem.getModule().getID(), moduleId)) {
                                    found = true;
                                    removeSlotIndex.add(i);
                                    break;
                                }
                            }
                        }
                        if (!found && !player.isCreative()) {
                            msg = Component.translatable("packet.set_module_tree.item_unmatched").getString();
                            msg = msg
                                    .replace("$id", newModule.getLong("id") + "")
                                    .replace("$moduleId", Component.translatable(newModule.getString("module")).getString());
                            GCRNetwork.sendToPlayer((ServerPlayer) player, new CommitModuleTreeResponsePacket(originalTag, msg, false));
                            return;
                        }
                    }
                    for (int i : removeSlotIndex) {
                        items.get(i).shrink(1);
                    }
                    for (Map.Entry<ModuleItem<?>, Integer> entry : preTakeoff.entrySet()) {
                        ModuleItem<?> moduleItem = entry.getKey();
                        for (int i = 0; i < entry.getValue(); i++) {
                            boolean add = player.getInventory().add(new ItemStack(moduleItem));
                            if (!add) {
                                ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(moduleItem));
                                player.level().addFreshEntity(entity);
                            }
                        }
                    }
                    gun.setModulesTag(mainHandItem, finalModules);

                    CompoundTag lastStates = gun.getStatesTag(mainHandItem);
                    ShadowNode shadowTree = warehouse.getShadowTree();
                    StatesUpdateContext statesUpdateContext = new StatesUpdateContext(gun, shadowTree, lastStates);
                    statesUpdateContext.autoExec();
                    gun.setStatesTag(mainHandItem, lastStates);

                    CompoundTag properties = gun.reCalculateProperties(warehouse);
                    gun.setPropertiesTag(mainHandItem, properties);

                    GCRNetwork.sendToPlayer((ServerPlayer) player, new CommitModuleTreeResponsePacket(
                            gunItem.getGun().checkAndGetRaw(mainHandItem), msg, true));

                } catch (Exception e) {
                    String msg = Component.translatable("packet.set_module_tree.exception").getString();
                    String message = e.getMessage();
                    msg = msg.replace("$e", (message == null ? "Unknown" : message));
                    GCRNetwork.sendToPlayer((ServerPlayer) player, new CommitModuleTreeResponsePacket(originalTag, msg, false));
                }
            }
        });
    }
    private static java.util.List<String> magazineNodes(ListTag tree) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (int i = 0; i < tree.size(); i++) {
            CompoundTag node = tree.getCompound(i);
            if ("MAG".equals(node.getString(Unit.SLOT_NAME))) {
                parts.add(node.getString(Unit.IN_TIME_ID) + ":" + node.getString(Unit.MODULE_ID));
            }
        }
        java.util.Collections.sort(parts);
        return parts;
    }
}
