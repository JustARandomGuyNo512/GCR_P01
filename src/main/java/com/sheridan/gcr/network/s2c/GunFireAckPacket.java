package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.Client;
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

public class GunFireAckPacket implements CustomPacketPayload, IPacket<GunFireAckPacket> {
    public static final ResourceLocation ID = GCR.RL("gun_fire_ack");
    public static final Type<GunFireAckPacket> TYPE = new Type<>(ID);
    public static final Codec<GunFireAckPacket> STREAM_CODEC = new Codec<> (
            GunFireAckPacket::decode,
            (buf, p) -> p.encode(buf));

    public String gunId;
    public int ammoLeft;
    public int shootId;
    public boolean stuck;
    /**
     * 服务端是否真的打出了这一发。
     *
     * <p>{@code false} 表示这一发被拒（服务端已经卡壳，或者客户端上报的枪不是当前主手）。
     * 客户端用它和本地预测配对：只要服务端处理过这一发就一定回执，客户端就不需要靠超时自愈。</p>
     */
    public boolean fired;

    public GunFireAckPacket(String gunId, int ammoLeft, int shootId, boolean stuck, boolean fired) {
        this.gunId = gunId;
        this.ammoLeft = ammoLeft;
        this.shootId = shootId;
        this.stuck = stuck;
        this.fired = fired;
    }

    private static GunFireAckPacket decode(FriendlyByteBuf buf) {
        return new GunFireAckPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeInt(ammoLeft);
        buf.writeInt(shootId);
        buf.writeBoolean(stuck);
        buf.writeBoolean(fired);
    }

    @Override
    public void onClient(GunFireAckPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 先让卡壳缓存和服务端对齐：回执是按 gunId 路由的，不要求这把枪此刻在主手，
            // 因此在切枪之后才到的回执也能正确解除/确认本地预测。
            ClientGunStuckCache.get().onServerAck(packet.gunId, packet.shootId, packet.stuck, packet.fired);

            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
                ItemStack mainHand = player.getMainHandItem();
                IGun gun = gunItem.getGun();
                if (packet.gunId.equals(gun.getIdentityID(mainHand))) {
                    // 手上就是这把枪：沿用原有路径把服务端权威状态写回物品 states
                    Client.serverShootAck(packet, mainHand, gun);
                }
            }
        });
    }

    @Override
    public void onServer(GunFireAckPacket packet, IPayloadContext context) {

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
