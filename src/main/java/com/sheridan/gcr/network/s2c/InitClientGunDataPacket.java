package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class InitClientGunDataPacket implements IPacket<InitClientGunDataPacket> {
    public static final ResourceLocation ID = GCR.RL("init_client_gun_data");

    public String moduleId;
    public int itemId;
    public CompoundTag data;

    public InitClientGunDataPacket(String moduleId, int itemId, CompoundTag data) {
        this.moduleId = moduleId;
        this.itemId = itemId;
        this.data = data;
    }

    public static InitClientGunDataPacket decode(FriendlyByteBuf buf) {
        return new InitClientGunDataPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readNbt()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(moduleId);
        buf.writeInt(itemId);
        buf.writeNbt(data);
    }

    @Override
    public void onClient(InitClientGunDataPacket packet, PacketContext context) {
        context.enqueueWork(() -> Client.onReceivedGunDataFromServer(packet));
    }

    @Override
    public void onServer(InitClientGunDataPacket packet, PacketContext context) {

    }}