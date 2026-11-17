package com.sheridan.gcr.client;

import com.sheridan.gcr.items.AmmoItem;
import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.network.c2s.MagLoadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class MagLoadingHandler {
    private static boolean guiLoading = false;
    private static int loadingSlotIndex = -1;
    private static ItemStack loadingMagStack = ItemStack.EMPTY;
    private static int currentTick = 0;
    public static final int CYCLE_TICKS = 12; // 0.6s per round
    private static String caliber = "";

    public static boolean isGuiLoading() {
        return guiLoading;
    }

    public static boolean isLoading() {
        return guiLoading;
    }

    public static int getLoadingSlotIndex() {
        return loadingSlotIndex;
    }

    public static boolean isTargetStack(ItemStack stack) {
        if (!guiLoading || stack == null || stack.isEmpty()) return false;
        if (loadingMagStack == stack) return true;
        return ItemStack.matches(loadingMagStack, stack);
    }

    public static float getProgress(float partialTicks) {
        if (!guiLoading) return 0.0f;
        return ((currentTick % CYCLE_TICKS) + partialTicks) / (float) CYCLE_TICKS;
    }

    public static void startGuiLoading(int slotIndex, String cal, ItemStack mag) {
        if (mag == null || mag.isEmpty() || !(mag.getItem() instanceof MagItem magItem)) {
            return;
        }
        guiLoading = true;
        loadingSlotIndex = slotIndex;
        loadingMagStack = mag;
        currentTick = 0;
        caliber = (cal != null && !cal.isEmpty()) ? cal : magItem.getCaliber();
    }

    public static void stopLoading(boolean notifyServer) {
        if (!guiLoading) return;
        guiLoading = false;
        loadingSlotIndex = -1;
        loadingMagStack = ItemStack.EMPTY;
        currentTick = 0;
        caliber = "";
        if (notifyServer) {
            PacketDistributor.sendToServer(new MagLoadPacket(MagLoadPacket.ACTION_STOP, ""));
        }
    }

    public static void tickClient(LocalPlayer player) {
        if (!guiLoading) return;

        Minecraft mc = Minecraft.getInstance();
        if (player == null || !player.isAlive()) {
            stopLoading(true);
            return;
        }

        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            stopLoading(true);
            return;
        }

        if (loadingSlotIndex < 0 || loadingSlotIndex >= containerScreen.getMenu().slots.size()) {
            stopLoading(true);
            return;
        }

        Slot slot = containerScreen.getMenu().slots.get(loadingSlotIndex);
        if (slot == null || !(slot.getItem().getItem() instanceof MagItem magItem)) {
            stopLoading(true);
            return;
        }

        ItemStack heldMag = slot.getItem();
        loadingMagStack = heldMag;
        int ammoLeft = MagItem.getAmmoLeft(heldMag);
        int maxCap = magItem.getModule().getMaxCapacity();

        if (ammoLeft >= maxCap) {
            stopLoading(true);
            return;
        }

        // Check if player has compatible ammo
        Inventory inv = player.getInventory();
        int ammoSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.getItem() instanceof AmmoItem ai && magItem.acceptsAmmo(ai)) {
                ammoSlot = i;
                break;
            }
        }

        if (ammoSlot < 0) {
            stopLoading(true);
            return;
        }

        currentTick++;

        // Insertion when tick % CYCLE_TICKS == 0
        if (currentTick % CYCLE_TICKS == 0) {
            PacketDistributor.sendToServer(new MagLoadPacket(MagLoadPacket.ACTION_LOAD_ROUND_GUI, caliber, loadingSlotIndex, false));
            MagItem.setAmmoLeft(heldMag, ammoLeft + 1);
        }
    }
}
