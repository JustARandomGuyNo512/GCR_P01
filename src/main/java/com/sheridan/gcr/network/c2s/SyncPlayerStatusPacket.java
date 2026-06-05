package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.data.PlayerStatusEvents;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncPlayerStatusPacket implements CustomPacketPayload, IPacket<SyncPlayerStatusPacket> {
    public static final ResourceLocation ID = GCR.RL("sync_player_status");
    public static final Type<SyncPlayerStatusPacket> TYPE = new Type<>(ID);
    public static final Codec<SyncPlayerStatusPacket> STREAM_CODEC = new Codec<> (
            SyncPlayerStatusPacket::decode,
            (buf, p) -> p.encode(buf));
    public long gcrCredit;
    public boolean isReloading;

    public SyncPlayerStatusPacket(long gcrCredit, boolean isReloading) {
        this.gcrCredit = gcrCredit;
        this.isReloading = isReloading;
    }

    private static SyncPlayerStatusPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerStatusPacket(
                buf.readLong(),
                buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(gcrCredit);
        buf.writeBoolean(isReloading);
    }

    @Override
    public void onClient(SyncPlayerStatusPacket packet, IPayloadContext context) {}

    @Override
    public void onServer(SyncPlayerStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerStatusEvents.serverReceivedClientSync((ServerPlayer) player, packet);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
