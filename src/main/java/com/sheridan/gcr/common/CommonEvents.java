package com.sheridan.gcr.common;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.s2c.InitClientGunDataPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonEvents {

    /** 数据版本迁移失败时给玩家的提示，$count 为返还的模块数量。 */
    private static final String MESSAGE_RESET_KEY = "gcr.message.gun_data_reset";

    /** 数据版本迁移时模块被裁剪后给玩家的提示，$count 为返还的模块数量。 */
    private static final String MESSAGE_PRUNED_KEY = "gcr.message.gun_data_pruned";

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
                gun.setDataDate(eventTo, serverStartTime);
                String initID = gun.getIdentityID(eventTo);
                if (!IGun.NONE.equals(initID) && event.getEntity() instanceof ServerPlayer player) {
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
                ListTag modulesTag = gun.getModulesTag(eventTo);
                List<String> oldIdList = collectModuleIds(gun, eventTo);
                IBuilder builder = new Builder();
                builder.initAndClear(modulesTag);
                try {
                    List<ValidateResult> commit = builder.commit();
                    boolean hasError = false;
                    for (ValidateResult result : commit) {
                        if (!result.isCommitAllowed()) {
                            hasError = true;
                            break;
                        }
                    }

                    IReadOnlyTree warehouse = builder.getWarehouse();
                    // 复制一份再交出去：finalModules 是 builder 仓库内部的引用，不能让它泄漏到物品数据里
                    ListTag finalModules = warehouse.write().copy();

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
                    if (hasError) {
                        // 提交本身已经把非法模块裁掉了，但被裁掉的配件还留在玩家背包外——补一次返还。
                        // 用 initAndClear 之前就抓好的 oldIdList 做差集，避免读被清空的 ListTag。
                        handlePrunedModulesRefund(gun, eventTo, oldIdList, finalModules, event.getEntity());
                    }
                } catch (Exception e) {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        handleGunDataRecovery(gunItem, player, oldIdList, e);
                    } else {
                        GCR.LOGGER.error("Failed to migrate gun {} data for non-player entity {}, "
                                + "no recovery is possible on this path", gun.getID(), event.getEntity(), e);
                    }
                }
            }
        }
    }



    /**
     * 枪械数据版本迁移失败时的兜底处理。
     *
     * <p>旧数据已经无法挽救，所以这里不是"修"而是"换"：<b>新建</b>一个同款枪物品的 ItemStack，
     * 在它身上生成全新的白板数据（并刷新 identityID），再把它塞回玩家手上。
     * 保证玩家手里至少有一把能用的枪，不会因为一个坏节点把存档卡死。
     * 旧枪上装过的模块则做"差集返还"——凡是新白板枪没有装配到的模块，都变回 ItemStack 还给玩家
     * （背包放不下就掉在脚下）；注册表里查不到的模块 id 直接丢弃。</p>
     *
     * <p><b>为什么不复用坏 stack</b>：{@code serverInitData} 走的是 {@code checkAndGetRaw}，
     * 而后者在 CUSTOM_DATA 非空时直接返回脏数据本身，所以拿坏 stack 去"重新初始化"等于什么都没做，
     * 结果是差集比对时新旧模块列表完全一样、返还 0 个配件。必须换一个新的 ItemStack 才能拿到干净母本。</p>
     *
     * <p><b>注意次生代价</b>：替换白板枪意味着旧枪上的模块一旦无法对应到注册表就会被丢弃，
     * 这是刻意的取舍——让坏数据继续留在存档里，代价比丢几个配件大得多。</p>
     *
     * @param gunItem 出问题的枪械物品
     * @param player  持有者（数据修复只对真实玩家有意义）
     * @param brokenModuleIds 旧枪上装配的模块 id 列表，只用于读取旧模块列表，不会被修改
     * @param cause   触发恢复的原始异常，仅用于日志
     */
    private static void handleGunDataRecovery(GunItem gunItem, ServerPlayer player, List<String> brokenModuleIds, Throwable cause) {
        IGun gun = gunItem.getGun();
        long serverStartTime = Commons.getServerStartTime();

        // 新建一个同款枪 ItemStack 并生成全新的白板数据。
        //    不能在 brokenStack 上原地重写：checkAndGetRaw 会直接返回已有的脏 CUSTOM_DATA，
        //    identityID 刷不掉、模块列表也不会变。
        ItemStack freshGun = new ItemStack(gunItem);
        // 初始数据里的 identityID 还是哨兵值，这里补上真正的随机 id（内部会把新 tag 写回 freshGun）
        gun.serverInitData(freshGun);
        CompoundTag freshTag = gun.checkAndGetRaw(freshGun);
        // 数据版本标记跟上本次开服时间，否则下次装备时会再次被判为过期数据
        gun.setDataDate(freshGun, serverStartTime);
        //  差集返还：白板枪没有装配到的模块，还给玩家
        List<ItemStack> refunds = collectModuleRefunds(brokenModuleIds, collectModuleIds(gun, freshGun));
        int refunded = giveModuleItems(player, refunds);

        //  直接把新枪塞回玩家主手：setItemInHand 换掉的就是玩家当前选中的那个背包槽位，
        //    物品变更会随容器同步自动下发，客户端拿到新 stack 后自行更新渲染，不需要额外发包
        player.setItemInHand(InteractionHand.MAIN_HAND, freshGun);

        GCR.LOGGER.error("Gun {} data migration failed for {}, the item was reset to a fresh one "
                        + "and {} extra module(s) were refunded (fresh data: {} bytes)",
                gun.getID(), player.getGameProfile().getName(), refunded, freshTag.sizeInBytes(), cause);

        player.sendSystemMessage(Component.literal(
                Component.translatable(MESSAGE_RESET_KEY).getString().replace("$count", Integer.toString(refunded))));
    }

    /**
     * 处理「数据可提交但有非法模块被裁剪」的情况：把裁掉的模块变回物品还给玩家，并提示。
     *
     * <p>和 {@link #handleGunDataRecovery} 的区别是这里<b>不重置枪械</b>——裁剪出来的数据是合法的，
     * 枪继续用，只是少了几个装不上的配件，所以只需要补发配件。</p>
     *
     * <p><b>为什么要用 {@code oldIdList}</b>：模块列表在 {@code builder.initAndClear(modulesTag)}
     * 时就被原地清空了，之后再从 {@code eventTo} 读只能拿到空列表。{@code oldIdList} 是在清空前抽出来的
     * 纯字符串快照，既不受 ListTag 引用对象被改写的影响，也不受业务数据污染，是唯一可靠的新旧比对依据。</p>
     *
     * @param gun         当前枪械模块
     * @param itemStack   玩家手上这把枪（裁剪后的数据已经写回它）
     * @param oldIdList   裁剪前的模块 id 快照
     * @param finalModules 裁剪后的模块列表
     * @param entity      触发本次装备变更的实体，非玩家时只记日志
     */
    private static void handlePrunedModulesRefund(IGun gun, ItemStack itemStack, List<String> oldIdList,
                                                  ListTag finalModules, LivingEntity entity) {
        List<String> newIdList = collectModuleIds(finalModules);
        List<ItemStack> refunds = collectModuleRefunds(oldIdList, newIdList);
        int refunded = 0;
        if (entity instanceof Player player) {
            refunded = giveModuleItems(player, refunds);
            if (refunded > 0) {
                player.sendSystemMessage(Component.literal(Component.translatable(MESSAGE_PRUNED_KEY)
                        .getString().replace("$count", Integer.toString(refunded))));
            }
        }
        int pruned = oldIdList.size() - newIdList.size();
        GCR.LOGGER.warn("Gun {} data version migration pruned {} module(s) ({} -> {}), refunded {}",
                gun.getID(), pruned, oldIdList.size(), newIdList.size(), refunded);
    }

    /**
     * 把模块物品发给玩家：优先塞背包，塞不下就掉在脚下。
     *
     * @return 实际发放的数量
     */
    private static int giveModuleItems(Player player, List<ItemStack> items) {
        int given = 0;
        for (ItemStack item : items) {
            if (!player.getInventory().add(item)) {
                ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), item);
                player.level().addFreshEntity(entity);
            }
            given++;
        }
        return given;
    }

    /** 取出枪械数据里所有模块的 id（含根节点自身的模块）。 */
    private static List<String> collectModuleIds(IGun gun, ItemStack itemStack) {
        return collectModuleIds(gun.getModulesTag(itemStack));
    }

    /** 从一个模块列表里取出所有模块 id。 */
    private static List<String> collectModuleIds(ListTag modules) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < modules.size(); i++) {
            CompoundTag moduleTag = modules.getCompound(i);
            String moduleId = moduleTag.getString(Unit.MODULE_ID);
            if (!moduleId.isEmpty()) {
                ids.add(moduleId);
            }
        }
        return ids;
    }

    /**
     * 计算需要返还的模块：白板枪没有装配到的那些。
     *
     * <p>按多重集做差，因此同一模块装了多个时只返还多出来的部分；注册表里查不到 id 的模块直接丢弃。</p>
     */
    private static List<ItemStack> collectModuleRefunds(List<String> brokenModuleIds, List<String> freshModuleIds) {
        Map<String, Integer> remaining = new HashMap<>();
        freshModuleIds.forEach(id -> remaining.merge(id, 1, Integer::sum));

        List<ItemStack> refunds = new ArrayList<>();
        for (String moduleId : brokenModuleIds) {
            Integer left = remaining.get(moduleId);
            if (left != null && left > 0) {
                // 白板枪上也装了同一个模块，互相抵消
                remaining.put(moduleId, left - 1);
                continue;
            }
            IModular module = ModuleRegister.get(moduleId);
            if (module == null) {
                GCR.LOGGER.warn("Dropping unknown module '{}' while refunding broken gun data", moduleId);
                continue;
            }
            if (module.getBindItem() == null) {
                GCR.LOGGER.warn("Dropping module '{}' while refunding broken gun data: it has no bound item", moduleId);
                continue;
            }
            refunds.add(new ItemStack(module.getBindItem()));
        }
        return refunds;
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
