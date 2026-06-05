package com.sheridan.gcr.network.s2c;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.screen.ldlib2Remake.GunModifyScreen;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class CommitModuleTreeResponsePacket implements CustomPacketPayload, IPacket<CommitModuleTreeResponsePacket> {
    public static final ResourceLocation ID = GCR.RL("set_module_tree_resp");
    public static final Type<CommitModuleTreeResponsePacket> TYPE = new Type<>(ID);
    public static final Codec<CommitModuleTreeResponsePacket> STREAM_CODEC = new Codec<> (
            CommitModuleTreeResponsePacket::decode,
            (buf, p) -> p.encode(buf));

    private static CommitModuleTreeResponsePacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new CommitModuleTreeResponsePacket(
                (CompoundTag) friendlyByteBuf.readNbt(NbtAccounter.unlimitedHeap()),
                friendlyByteBuf.readUtf(),
                friendlyByteBuf.readBoolean());
    }

    public CompoundTag data;
    public String msg;
    public boolean success;

    public CommitModuleTreeResponsePacket(CompoundTag data, String msg, boolean success) {
        this.data = data;
        this.msg = msg;
        this.success = success;
    }

    @Override
    public void onClient(CommitModuleTreeResponsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                gun.fullSyncFromServer(player.getMainHandItem(), packet.data);
            }
            if (instance.screen instanceof GunModifyScreen screen) {
                screen.onServerResp(packet.success, packet.msg);
            }
        });
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
        buf.writeUtf(msg);
        buf.writeBoolean(success);
    }

    @Override
    public void onServer(CommitModuleTreeResponsePacket packet, IPayloadContext context) {

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
