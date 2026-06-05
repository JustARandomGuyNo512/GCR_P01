package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.ISlottedGun;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SwitchUsingSightPacket implements CustomPacketPayload, IPacket<SwitchUsingSightPacket> {
    public static final ResourceLocation ID = GCR.RL("switch_using_sight");
    public static final Type<SwitchUsingSightPacket> TYPE = new Type<>(ID);
    public static final Codec<SwitchUsingSightPacket> STREAM_CODEC = new Codec<> (
            SwitchUsingSightPacket::decode,
            (buf, p) -> p.encode(buf));

    private void encode(FriendlyByteBuf buf) {

    }

    private static SwitchUsingSightPacket decode(FriendlyByteBuf buf) {
        return new SwitchUsingSightPacket();
    }

    @Override
    public void onClient(SwitchUsingSightPacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(SwitchUsingSightPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem && gunItem.getGun() instanceof ISlottedGun slottedGun) {
                if (slottedGun.switchUsingSight(heldItem)) {
                    gunItem.getGun().notifyDataChanged(heldItem);
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
