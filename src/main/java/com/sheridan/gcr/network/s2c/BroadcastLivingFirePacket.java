package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class BroadcastLivingFirePacket implements CustomPacketPayload, IPacket<BroadcastLivingFirePacket> {
    public static final ResourceLocation ID = GCR.RL("broadcast_living_fire");
    public static final Type<BroadcastLivingFirePacket> TYPE = new Type<>(ID);
    public static final Codec<BroadcastLivingFirePacket> STREAM_CODEC = new Codec<> (
            BroadcastLivingFirePacket::decode,
            (buf, p) -> p.encode(buf));

    public int entityId;
    public String fireModuleId;
    public String gunId;
    public int latency;

    public BroadcastLivingFirePacket(int entityId, String fireModuleId, String gunId, int latency) {
        this.entityId = entityId;
        this.fireModuleId = fireModuleId;
        this.gunId = gunId;
        this.latency = latency;
    }

    private static BroadcastLivingFirePacket decode(FriendlyByteBuf buf) {
        return new BroadcastLivingFirePacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(fireModuleId);
        buf.writeUtf(gunId);
        buf.writeInt(latency);
    }

    @Override
    public void onClient(BroadcastLivingFirePacket packet, IPayloadContext context) {
        Client.handleLivingFire(packet, context);
    }

    @Override
    public void onServer(BroadcastLivingFirePacket packet, IPayloadContext context) {

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int entityId() {
        return entityId;
    }
}
