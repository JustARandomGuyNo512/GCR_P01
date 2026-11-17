package com.sheridan.gcr.client;

import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.items.AmmoItem;
import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.network.c2s.MagLoadPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class MagLoadingHandler {
    private static boolean loading = false;
    private static int currentTick = 0;
    private static boolean keyReleased = false;
    private static int initialGraceTicks = 0;
    public static final int CYCLE_TICKS = 12;
    private static ItemStack magStack = ItemStack.EMPTY;
    private static ItemStack ammoStack = ItemStack.EMPTY;
    private static String caliber = "";

    public static boolean isLoading() {
        return loading;
    }

    public static int getCurrentTick() {
        return currentTick;
    }

    public static ItemStack getMagStack() {
        return magStack;
    }

    public static ItemStack getAmmoStack() {
        return ammoStack;
    }

    public static void startLoading(ItemStack mag, String cal) {
        if (mag == null || mag.isEmpty() || !(mag.getItem() instanceof MagItem magItem)) {
            return;
        }
        loading = true;
        currentTick = 0;
        keyReleased = false;
        initialGraceTicks = 6; // Ignore mouse click for 6 ticks (~300ms)
        magStack = mag;
        caliber = (cal != null && !cal.isEmpty()) ? cal : magItem.getCaliber();
        AmmoItem ai = AmmoItem.getByCaliber(caliber);
        ammoStack = ai != null ? new ItemStack(ai) : ItemStack.EMPTY;
    }

    public static void stopLoading(boolean notifyServer) {
        if (!loading) return;
        loading = false;
        currentTick = 0;
        keyReleased = false;
        initialGraceTicks = 0;
        magStack = ItemStack.EMPTY;
        ammoStack = ItemStack.EMPTY;
        if (notifyServer) {
            PacketDistributor.sendToServer(new MagLoadPacket(MagLoadPacket.ACTION_STOP, ""));
        }
    }

    public static void tickClient(LocalPlayer player) {
        if (!loading) return;

        Minecraft mc = Minecraft.getInstance();
        if (player == null || !player.isAlive()) {
            stopLoading(true);
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof MagItem magItem)) {
            stopLoading(true);
            return;
        }
        magStack = held;

        // Initial click release & grace period
        if (initialGraceTicks > 0) {
            initialGraceTicks--;
        }
        if (!mc.options.keyAttack.isDown() && !mc.options.keyUse.isDown()) {
            keyReleased = true;
        }

        // Cancel on new click after initial click released, sprint or reload key
        if (initialGraceTicks == 0 && keyReleased) {
            if (mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) {
                stopLoading(true);
                return;
            }
        }
        if (KeyBinds.RELOAD.isDown() || player.isSprinting()) {
            stopLoading(true);
            return;
        }

        int ammoLeft = MagItem.getAmmoLeft(held);
        int maxCap = magItem.getModule().getMaxCapacity();

        if (ammoLeft >= maxCap) {
            player.displayClientMessage(Component.translatable("gcr.overlay.mag_full").withStyle(ChatFormatting.GREEN), true);
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
                ammoStack = s.copy();
                break;
            }
        }

        if (ammoSlot < 0) {
            player.displayClientMessage(Component.translatable("gcr.overlay.no_ammo_for_mag").withStyle(ChatFormatting.RED), true);
            stopLoading(true);
            return;
        }

        currentTick++;

        // Insertion at tick 6 of 12
        if (currentTick % CYCLE_TICKS == 6) {
            PacketDistributor.sendToServer(new MagLoadPacket(MagLoadPacket.ACTION_LOAD_ROUND, caliber));

            MagItem.setAmmoLeft(held, ammoLeft + 1);

            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.7f, 1.35f);
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.1f);

            player.displayClientMessage(Component.translatable("gcr.overlay.loading_mag", (ammoLeft + 1), maxCap).withStyle(ChatFormatting.GOLD), true);
        }
    }
}
