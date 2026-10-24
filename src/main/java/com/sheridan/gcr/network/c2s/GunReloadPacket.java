package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.sheridan.gcr.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class GunReloadPacket implements IPacket<GunReloadPacket> {
    public static final ResourceLocation ID = GCR.RL("gun_reload");

    public void encode(FriendlyByteBuf buf) {

    }

    public static GunReloadPacket decode(FriendlyByteBuf buf) {
        return new GunReloadPacket();
    }

    @Override
    public void onClient(GunReloadPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(GunReloadPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            // Disabled: legacy reload created ammunition without cartridge items.
            // Physical reload is handled by PhysicalMagazinePacket.
        });
    }}

