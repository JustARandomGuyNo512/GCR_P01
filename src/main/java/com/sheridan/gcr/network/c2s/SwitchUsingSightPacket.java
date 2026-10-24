package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.modules.guns.ISlottedGun;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.sheridan.gcr.network.PacketContext;

public class SwitchUsingSightPacket implements IPacket<SwitchUsingSightPacket> {
    public static final ResourceLocation ID = GCR.RL("switch_using_sight");

    public void encode(FriendlyByteBuf buf) {

    }

    public static SwitchUsingSightPacket decode(FriendlyByteBuf buf) {
        return new SwitchUsingSightPacket();
    }

    @Override
    public void onClient(SwitchUsingSightPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(SwitchUsingSightPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem && gunItem.getGun() instanceof ISlottedGun slottedGun) {
                if (slottedGun.switchUsingSight(heldItem)) {
                    gunItem.getGun().notifyDataChanged(heldItem);
                }
            }
        });
    }}
