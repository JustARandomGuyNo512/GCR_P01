package com.sheridan.gcr.client;

import com.sheridan.gcr.items.AmmoItem;
import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.network.c2s.MagLoadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

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

    public static void handleMagLoadingClick(AbstractContainerScreen<?> screen, Slot slot, boolean isCarriedMag, String caliber) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack carried = screen.getMenu().getCarried();
        screen.getMenu().setCarried(ItemStack.EMPTY);

        int selectedHotbar = player.getInventory().selected;
        ItemStack oldHeld = player.getInventory().getItem(selectedHotbar);
        ItemStack magStack = ItemStack.EMPTY;

        if (isCarriedMag) {
            magStack = carried.copy();
            if (!oldHeld.isEmpty()) {
                if (!player.getInventory().add(oldHeld)) {
                    player.drop(oldHeld, false);
                }
            }
            player.getInventory().setItem(selectedHotbar, magStack);
        } else if (slot != null && slot.getItem().getItem() instanceof MagItem) {
            magStack = slot.getItem().copy();
            if (!carried.isEmpty()) {
                if (!player.getInventory().add(carried)) {
                    player.drop(carried, false);
                }
            }
            slot.set(oldHeld);
            player.getInventory().setItem(selectedHotbar, magStack);
        }

        // Send packet to server to swap slots and close container
        int slotIndex = slot != null ? slot.index : -1;
        PacketDistributor.sendToServer(new MagLoadPacket(MagLoadPacket.ACTION_START_FROM_SLOT, caliber, slotIndex, isCarriedMag));

        // Close screen
        mc.setScreen(null);

        // Start local loading animation and loop
        MagLoadingHandler.startLoading(magStack, caliber);
    }
}
