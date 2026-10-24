package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class SyncHeatDataPacket implements IPacket<SyncHeatDataPacket> {
    public static final ResourceLocation ID = GCR.RL("sync_heat");

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

    public static SyncHeatDataPacket decode(FriendlyByteBuf buf) {
        return new SyncHeatDataPacket(
                buf.readUtf(),
                buf.readFloat(),
                buf.readLong(),
                buf.readLong()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeFloat(heat);
        buf.writeLong(lastHeatUpdateTime);
        buf.writeLong(lastShootTime);
    }

    @Override
    public void onClient(SyncHeatDataPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            //Client.syncGunHeatData( packet);
        });
    }

    @Override
    public void onServer(SyncHeatDataPacket packet, PacketContext context) {

    }}