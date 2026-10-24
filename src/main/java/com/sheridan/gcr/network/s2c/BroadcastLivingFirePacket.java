package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class BroadcastLivingFirePacket implements IPacket<BroadcastLivingFirePacket> {
    public static final ResourceLocation ID = GCR.RL("broadcast_living_fire");

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

    public static BroadcastLivingFirePacket decode(FriendlyByteBuf buf) {
        return new BroadcastLivingFirePacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(fireModuleId);
        buf.writeUtf(gunId);
        buf.writeInt(latency);
    }

    @Override
    public void onClient(BroadcastLivingFirePacket packet, PacketContext context) {
        Client.handleLivingFire(packet, context);
    }

    @Override
    public void onServer(BroadcastLivingFirePacket packet, PacketContext context) {

    }    public int entityId() {
        return entityId;
    }
}
