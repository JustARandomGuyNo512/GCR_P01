package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.network.s2c.GunStuckSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端清障动作完成，请求服务端解除卡壳。
 *
 * <p>带上 gunId 有两个原因：一是玩家可能在清障动画期间切枪，服务端必须去背包里找对那一把，
 * 而不是把解除作用到当前主手武器上；二是服务端处理完必须回 {@link GunStuckSyncPacket}，
 * 客户端用它了结本地预测——客户端在自认为卡壳时不会开火，不能等下一次开火被拒才同步。</p>
 */
public class RemoveStuckPacket implements CustomPacketPayload, IPacket<RemoveStuckPacket> {
    public static final ResourceLocation ID = GCR.RL("remove_stuck");
    public static final Type<RemoveStuckPacket> TYPE = new Type<>(ID);
    public static final Codec<RemoveStuckPacket> STREAM_CODEC = new Codec<> (
            RemoveStuckPacket::decode,
            (buf, p) -> p.encode(buf));

    public String gunId;

    public RemoveStuckPacket(String gunId) {
        this.gunId = gunId;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
    }

    private static RemoveStuckPacket decode(FriendlyByteBuf buf) {
        return new RemoveStuckPacket(buf.readUtf());
    }

    @Override
    public void onClient(RemoveStuckPacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(RemoveStuckPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack target = findGun(player, packet.gunId);
            if (target.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new GunStuckSyncPacket(packet.gunId, false));
                return;
            }
            IGun gun = ((GunItem) target.getItem()).getGun();
            if (gun.isStuck(target)) {
                gun.removeStuck(target);
                gun.notifyDataChanged(target);
            }

            PacketDistributor.sendToPlayer(player, new GunStuckSyncPacket(packet.gunId, gun.isStuck(target)));
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
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
