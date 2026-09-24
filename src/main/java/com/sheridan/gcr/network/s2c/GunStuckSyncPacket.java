package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端 → 客户端的卡壳状态同步。
 *
 * <p>开火路径上的卡壳由 {@link GunFireAckPacket} 带回，本包只覆盖<b>非开火路径</b>的卡壳变化，
 * 目前就是「清障 / 排障把卡壳解除了」。没有它的话，客户端只能等下一次开火被拒才知道故障已经
 * 解除，而客户端在自认为卡壳时根本不会开火——就会卡死在这个死循环里。</p>
 *
 * <p>服务端处理 {@code RemoveStuckPacket} 后<b>无论是否真的解除过</b>都会回这个包：
 * 客户端据此把本地预测和已确认记录一起了结，这是“虚假卡壳”的最后一道保险。</p>
 */
public class GunStuckSyncPacket implements CustomPacketPayload, IPacket<GunStuckSyncPacket> {
    public static final ResourceLocation ID = GCR.RL("gun_stuck_sync");
    public static final Type<GunStuckSyncPacket> TYPE = new Type<>(ID);
    public static final Codec<GunStuckSyncPacket> STREAM_CODEC = new Codec<> (
            GunStuckSyncPacket::decode,
            (buf, p) -> p.encode(buf));

    public String gunId;
    public boolean stuck;

    public GunStuckSyncPacket(String gunId, boolean stuck) {
        this.gunId = gunId;
        this.stuck = stuck;
    }

    private static GunStuckSyncPacket decode(FriendlyByteBuf buf) {
        return new GunStuckSyncPacket(buf.readUtf(), buf.readBoolean());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeBoolean(stuck);
    }

    @Override
    public void onClient(GunStuckSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientGunStuckCache.get().onServerSync(packet.gunId, packet.stuck);

            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player == null) {
                return;
            }
            ItemStack mainHand = player.getMainHandItem();
            if (!(mainHand.getItem() instanceof GunItem gunItem)) {
                return;
            }
            IGun gun = gunItem.getGun();
            if (packet.gunId.equals(gun.getIdentityID(mainHand))) {
                gun.setStuck(packet.stuck, gun.rootNodeTag(mainHand));
            }
        });
    }

    @Override
    public void onServer(GunStuckSyncPacket packet, IPayloadContext context) {

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
