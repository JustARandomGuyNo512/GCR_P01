package com.sheridan.gcr.common;

import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.s2c.InitClientGunDataPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class CommonEvents {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player entity = event.getEntity();
        if (!entity.level().isClientSide) {
            handleDataChanged(entity.getMainHandItem());
        }
    }

    private static void handleDataChanged(ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem && gunItem.getGun().dataChanged(itemStack)) {
            CustomData original = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (!original.isEmpty()) {
                CompoundTag newData = original.copyTag();
                newData.putBoolean(IGun.DATA_CHANGED_KEY, false);
                CustomData.set(DataComponents.CUSTOM_DATA, itemStack, newData);
            }
        }
    }

    private static void handleGunDataUpdate(LivingEquipmentChangeEvent event) {
        ItemStack eventTo = event.getTo();
        if (eventTo.getItem() instanceof GunItem gunItem) {
            IGun gun = gunItem.getGun();
            long dataDate = gun.getDataDate(eventTo);
            String identityID = gun.getIdentityID(eventTo);
            long serverStartTime = Commons.getServerStartTime();
            if (IGun.NONE.equals(identityID)) {
                gun.serverInitData(eventTo);
                String initID = gun.getIdentityID(eventTo);
                if (!IGun.NONE.equals(initID) && event.getEntity() instanceof ServerPlayer player) {
                    //TODO:发送数据同步包给客户端
                    System.out.println("should send data to client");
                    PacketDistributor.sendToPlayer(
                            player,
                            new InitClientGunDataPacket(
                                    gun.getID(),
                                    Item.getId(gunItem),
                                    gun.checkAndGetRaw(eventTo)
                            )
                    );
                }
            } else if (dataDate != serverStartTime) {
                //String modifyID = gun.getStructureID(eventTo);
                ListTag modulesTag = gun.getModulesTag(eventTo);
                System.out.println(modulesTag);
                IBuilder builder = new Builder();
                builder.init(modulesTag);
                try {
                    List<ValidateResult> commit = builder.commit();
                    boolean hasError = false;
                    for (ValidateResult result : commit) {
                        if (!result.isCommitAllowed()) {
                            hasError = true;
                            break;
                        }
                    }
                    if (hasError && event.getEntity() instanceof Player player) {
                        player.sendSystemMessage(Component.literal("Data model error..."));
                    }
                    IReadOnlyTree warehouse = builder.getWarehouse();
                    ListTag finalModules = warehouse.write();

                    gun.setModulesTag(eventTo, finalModules);

                    CompoundTag lastStates = gun.getStatesTag(eventTo);
                    ShadowNode shadowTree = warehouse.getShadowTree();
                    StatesUpdateContext statesUpdateContext = new StatesUpdateContext(gun, shadowTree, lastStates);
                    statesUpdateContext.autoExec();
                    gun.setStatesTag(eventTo, lastStates);

                    CompoundTag properties = gun.reCalculateProperties(warehouse);
                    gun.setPropertiesTag(eventTo, properties);

                    gun.setDataDate(eventTo, serverStartTime);
                    gun.setModifyID(eventTo, gun.getModifyID(eventTo) + 1, false);
                    gun.notifyDataChanged(eventTo);
                } catch (Exception e) {

                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        handleGunDataUpdate(event);

        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || !(living instanceof Player)) {
            return;
        }
        handleDataChanged(event.getFrom());
        handleDataChanged(event.getTo());
    }

}
