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

@Deprecated
public class SyncHeatDataPacket implements CustomPacketPayload, IPacket<SyncHeatDataPacket> {
    public static final ResourceLocation ID = GCR.RL("sync_heat");
    public static final Type<SyncHeatDataPacket> TYPE = new Type<>(ID);
    public static final Codec<SyncHeatDataPacket> STREAM_CODEC = new Codec<> (
            SyncHeatDataPacket::decode,
            (buf, p) -> p.encode(buf));

    public String gunId;
    public float heat;
    public long lastHeatUpdateTime;
    public long lastShootTime;

    public SyncHeatDataPacket(String gunId, float heat, long lastHeatUpdateTime, long lastShootTime) {
        this.gunId = gunId;
        this.heat = heat;
        this.lastHeatUpdateTime = lastHeatUpdateTime;
        this.lastShootTime = lastShootTime;
    }

    private static SyncHeatDataPacket decode(FriendlyByteBuf buf) {
        return new SyncHeatDataPacket(
                buf.readUtf(),
                buf.readFloat(),
                buf.readLong(),
                buf.readLong()
        );
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeFloat(heat);
        buf.writeLong(lastHeatUpdateTime);
        buf.writeLong(lastShootTime);
    }

    @Override
    public void onClient(SyncHeatDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Client.syncGunHeatData( packet);
        });
    }

    @Override
    public void onServer(SyncHeatDataPacket packet, IPayloadContext context) {

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}