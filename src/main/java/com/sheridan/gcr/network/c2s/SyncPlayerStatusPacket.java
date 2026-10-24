package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.data.PlayerStatusEvents;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.sheridan.gcr.network.PacketContext;

public class SyncPlayerStatusPacket implements IPacket<SyncPlayerStatusPacket> {
    public static final ResourceLocation ID = GCR.RL("sync_player_status");
    public long gcrCredit;
    public boolean isReloading;

    public SyncPlayerStatusPacket(long gcrCredit, boolean isReloading) {
        this.gcrCredit = gcrCredit;
        this.isReloading = isReloading;
    }

    public static SyncPlayerStatusPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerStatusPacket(
                buf.readLong(),
                buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(gcrCredit);
        buf.writeBoolean(isReloading);
    }

    @Override
    public void onClient(SyncPlayerStatusPacket packet, PacketContext context) {}

    @Override
    public void onServer(SyncPlayerStatusPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerStatusEvents.serverReceivedClientSync((ServerPlayer) player, packet);
        });
    }}
