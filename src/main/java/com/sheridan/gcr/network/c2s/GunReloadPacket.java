package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class GunReloadPacket implements CustomPacketPayload, IPacket<GunReloadPacket> {
    public static final ResourceLocation ID = GCR.RL("gun_reload");
    public static final Type<GunReloadPacket> TYPE = new Type<>(ID);
    public static final Codec<GunReloadPacket> STREAM_CODEC = new Codec<> (
            GunReloadPacket::decode,
            (buf, p) -> p.encode(buf));

    private void encode(FriendlyByteBuf buf) {

    }

    private static GunReloadPacket decode(FriendlyByteBuf buf) {
        return new GunReloadPacket();
    }

    @Override
    public void onClient(GunReloadPacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(GunReloadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
                gunItem.getGun().reload(heldItem, player);
                gunItem.getGun().notifyDataChanged(heldItem);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

