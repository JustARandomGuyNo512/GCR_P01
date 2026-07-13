package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
    public long heatUpdateTime;
    public float heat;
    public boolean stuck;

    public GunFireAckPacket(String gunId, int ammoLeft, int shootId, boolean stuck, long heatUpdateTime, float heat) {
        this.gunId = gunId;
        this.ammoLeft = ammoLeft;
        this.shootId = shootId;
        this.stuck = stuck;
        this.heatUpdateTime = heatUpdateTime;
        this.heat = heat;
    }

    private static GunFireAckPacket decode(FriendlyByteBuf buf) {
        return new GunFireAckPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readLong(),
                buf.readFloat());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeInt(ammoLeft);
        buf.writeInt(shootId);
        buf.writeBoolean(stuck);
        buf.writeLong(heatUpdateTime);
        buf.writeFloat(heat);
    }

    @Override
    public void onClient(GunFireAckPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                if (packet.gunId.equals(gun.getIdentityID(player.getMainHandItem()))) {
                    Client.serverShootAck(packet, player.getMainHandItem(), gun);
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
