package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class RemoveStuckPacket implements CustomPacketPayload, IPacket<RemoveStuckPacket> {
    public static final ResourceLocation ID = GCR.RL("remove_stuck");
    public static final Type<RemoveStuckPacket> TYPE = new Type<>(ID);
    public static final Codec<RemoveStuckPacket> STREAM_CODEC = new Codec<> (
            RemoveStuckPacket::decode,
            (buf, p) -> p.encode(buf));

    private void encode(FriendlyByteBuf buf) {

    }

    private static RemoveStuckPacket decode(FriendlyByteBuf buf) {
        return new RemoveStuckPacket();
    }

    @Override
    public void onClient(RemoveStuckPacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(RemoveStuckPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                if (gun.isStuck(heldItem)) {
                    gun.removeStuck(heldItem);
                    gun.notifyDataChanged(heldItem);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
