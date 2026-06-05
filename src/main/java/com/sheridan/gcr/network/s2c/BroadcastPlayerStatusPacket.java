package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.data.PlayerStatusEvents;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class BroadcastPlayerStatusPacket implements CustomPacketPayload, IPacket<BroadcastPlayerStatusPacket> {
    public static final ResourceLocation ID = GCR.RL("broadcast_player_status");
    public static final Type<BroadcastPlayerStatusPacket> TYPE = new Type<>(ID);
    public static final Codec<BroadcastPlayerStatusPacket> STREAM_CODEC = new Codec<> (
            BroadcastPlayerStatusPacket::decode,
            (buf, p) -> p.encode(buf));

    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeLong(gcrCredit);
        buf.writeBoolean(isReloading);
    }

    private static BroadcastPlayerStatusPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new BroadcastPlayerStatusPacket(
                friendlyByteBuf.readInt(),
                friendlyByteBuf.readLong(),
                friendlyByteBuf.readBoolean());
    }

    public int entityID;
    public long gcrCredit;
    public boolean isReloading;

    public BroadcastPlayerStatusPacket(int entityID, long gcrCredit, boolean isReloading) {
        this.entityID = entityID;
        this.gcrCredit = gcrCredit;
        this.isReloading = isReloading;
    }

    @Override
    public void onClient(BroadcastPlayerStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PlayerStatusEvents.clientReceivedServerBroadcast(packet);
        });
    }

    @Override
    public void onServer(BroadcastPlayerStatusPacket packet, IPayloadContext context) {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
