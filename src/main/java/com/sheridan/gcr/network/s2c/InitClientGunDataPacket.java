package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class InitClientGunDataPacket implements CustomPacketPayload, IPacket<InitClientGunDataPacket> {
    public static final ResourceLocation ID = GCR.RL("init_client_gun_data");
    public static final Type<InitClientGunDataPacket> TYPE = new Type<>(ID);
    public static final Codec<InitClientGunDataPacket> STREAM_CODEC = new Codec<> (
            InitClientGunDataPacket::decode,
            (buf, p) -> p.encode(buf));

    public String moduleId;
    public int itemId;
    public CompoundTag data;

    public InitClientGunDataPacket(String moduleId, int itemId, CompoundTag data) {
        this.moduleId = moduleId;
        this.itemId = itemId;
        this.data = data;
    }

    private static InitClientGunDataPacket decode(FriendlyByteBuf buf) {
        return new InitClientGunDataPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readNbt()
        );
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(moduleId);
        buf.writeInt(itemId);
        buf.writeNbt(data);
    }

    @Override
    public void onClient(InitClientGunDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> Client.onReceivedGunDataFromServer(packet));
    }

    @Override
    public void onServer(InitClientGunDataPacket packet, IPayloadContext context) {

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}