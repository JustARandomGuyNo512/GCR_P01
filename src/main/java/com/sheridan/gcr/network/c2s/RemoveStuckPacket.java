package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.network.c2s.PhysicalMagazinePacket;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.network.s2c.GunStuckSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.sheridan.gcr.network.GCRNetwork;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端清障动作完成，请求服务端解除卡壳。
 *
 * <p>带上 gunId 有两个原因：一是玩家可能在清障动画期间切枪，服务端必须去背包里找对那一把，
 * 而不是把解除作用到当前主手武器上；二是服务端处理完必须回 {@link GunStuckSyncPacket}，
 * 客户端用它了结本地预测——客户端在自认为卡壳时不会开火，不能等下一次开火被拒才同步。</p>
 */
public class RemoveStuckPacket implements IPacket<RemoveStuckPacket> {
    public static final ResourceLocation ID = GCR.RL("remove_stuck");

    public String gunId;

    public RemoveStuckPacket(String gunId) {
        this.gunId = gunId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
    }

    public static RemoveStuckPacket decode(FriendlyByteBuf buf) {
        return new RemoveStuckPacket(buf.readUtf());
    }

    @Override
    public void onClient(RemoveStuckPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(RemoveStuckPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            // Physical-magazine mechanics: the server accepts clearing only after a held-R
            // authorization and an actual minimum clearing animation period.
            long started = player.getPersistentData().getLong(PhysicalMagazinePacket.JAM_START_KEY);
            String startedGun = player.getPersistentData().getString(PhysicalMagazinePacket.JAM_GUN_KEY);
            long elapsed = player.level().getGameTime() - started;
            ItemStack target = player.getMainHandItem();
            if (started <= 0L || elapsed < 7L || elapsed > 100L
                    || !packet.gunId.equals(startedGun)
                    || !(target.getItem() instanceof GunItem gunHeld)
                    || !packet.gunId.equals(gunHeld.getGun().getIdentityID(target))) {
                if (target.getItem() instanceof GunItem current) {
                    GCRNetwork.sendToPlayer(player,
                            new GunStuckSyncPacket(packet.gunId, current.getGun().isStuck(target)));
                }
                return;
            }
            player.getPersistentData().remove(PhysicalMagazinePacket.JAM_START_KEY);
            player.getPersistentData().remove(PhysicalMagazinePacket.JAM_GUN_KEY);
            IGun gun = ((GunItem) target.getItem()).getGun();
            if (gun.isStuck(target)) {
                gun.removeStuck(target);
                gun.notifyDataChanged(target);
            }

            GCRNetwork.sendToPlayer(player, new GunStuckSyncPacket(packet.gunId, gun.isStuck(target)));
        });
    }

    /**
     * 按 identityID 在玩家身上找那把枪：优先主手，其次背包。
     * 找不到返回 {@link ItemStack#EMPTY}。
     */
    private static ItemStack findGun(ServerPlayer player, String gunId) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof GunItem gunItem
                && gunId.equals(gunItem.getGun().getIdentityID(mainHand))) {
            return mainHand;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GunItem gunItem
                    && gunId.equals(gunItem.getGun().getIdentityID(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }}
