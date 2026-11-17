package com.sheridan.gcr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagClientBridge {
    public static void startLoadingFromInventory(String caliber) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            if (player.containerMenu != null) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            mc.setScreen(null);
            MagLoadingHandler.startLoading(player.getMainHandItem(), caliber);
        }
    }
}
