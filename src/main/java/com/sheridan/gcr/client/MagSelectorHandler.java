package com.sheridan.gcr.client;

import com.sheridan.gcr.client.render.SdfShader;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class MagSelectorHandler {
    private static boolean rPressed = false;
    private static int holdTicks = 0;
    public static final int HOLD_THRESHOLD = 5; // ~250ms hold
    private static boolean active = false;
    private static int selectedIndex = 0;
    public static final List<MagEntry> compatibleMags = new ArrayList<>();

    public static class MagEntry {
        public final int slot;
        public final ItemStack stack;
        public final MagItem item;
        public final int ammo;
        public final int maxCapacity;
        public final String name;
        public final String caliber;

        public MagEntry(int slot, ItemStack stack, MagItem item, int ammo, int maxCapacity, String name, String caliber) {
            this.slot = slot;
            this.stack = stack;
            this.item = item;
            this.ammo = ammo;
            this.maxCapacity = maxCapacity;
            this.name = name;
            this.caliber = caliber;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void refreshCompatibleMags(Player player, IGun gun) {
        compatibleMags.clear();
        if (player == null || gun == null) {
            return;
        }
        NonNullList<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.getItem() instanceof MagItem magItem) {
                if (SlottedGunMainPart.isMagCompatible(gun, magItem.getModule())) {
                    int ammo = MagItem.getAmmoLeft(stack);
                    int maxCap = magItem.getModule().getMaxCapacity();
                    String name = stack.getHoverName().getString();
                    String caliber = magItem.getCaliber();
                    compatibleMags.add(new MagEntry(i, stack, magItem, ammo, maxCap, name, caliber));
                }
            }
        }
        // Sort by ammo count descending (fullest magazines first)
        compatibleMags.sort((a, b) -> Integer.compare(b.ammo, a.ammo));
        selectedIndex = 0;
    }

    public static boolean onMouseScroll(double deltaY) {
        if (!active || compatibleMags.isEmpty()) {
            return false;
        }
        int prev = selectedIndex;
        int size = compatibleMags.size();
        if (deltaY > 0) {
            selectedIndex = (selectedIndex - 1 + size) % size;
        } else if (deltaY < 0) {
            selectedIndex = (selectedIndex + 1) % size;
        }
        if (prev != selectedIndex) {
            playClickSound();
        }
        return true;
    }

    private static void playClickSound() {
        try {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.35f)
            );
        } catch (Exception ignored) {}
    }

    public static void onReloadKeyPressed(LocalPlayer player, IGun gunModule, ItemStack itemStack) {
        if (GunTaskHandler.INSTANCE.hasTask()) {
            return;
        }
        rPressed = true;
        holdTicks = 0;
        active = false;
        refreshCompatibleMags(player, gunModule);
    }

    public static void onReloadKeyReleased(LocalPlayer player, IGun gunModule, ItemStack itemStack) {
        if (!rPressed) {
            return;
        }
        rPressed = false;

        if (active) {
            active = false;
            holdTicks = 0;
            if (!compatibleMags.isEmpty() && selectedIndex >= 0 && selectedIndex < compatibleMags.size()) {
                int targetSlot = compatibleMags.get(selectedIndex).slot;
                triggerReload(player, itemStack, gunModule, targetSlot);
            }
        } else {
            holdTicks = 0;
            triggerReload(player, itemStack, gunModule, -1);
        }
    }

    public static void reset() {
        rPressed = false;
        holdTicks = 0;
        active = false;
        compatibleMags.clear();
        selectedIndex = 0;
    }

    private static void triggerReload(LocalPlayer player, ItemStack itemStack, IGun gunModule, int targetSlot) {
        if (GunTaskHandler.INSTANCE.hasTask()) {
            return;
        }
        Map<String, Object> args = targetSlot >= 0 ? Map.of("targetSlot", targetSlot) : Map.of();
        IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.RELOAD, args);
        if (task != null) {
            GunTaskHandler.INSTANCE.setTask(task);
        }
    }

    public static void onClientTick(LocalPlayer player) {
        if (player == null || Minecraft.getInstance().screen != null) {
            if (rPressed || active) {
                reset();
            }
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            if (rPressed || active) {
                reset();
            }
            return;
        }

        if (rPressed) {
            if (!KeyBinds.RELOAD.isDown()) {
                onReloadKeyReleased(player, gunItem.getGun(), held);
                return;
            }
            holdTicks++;
            if (holdTicks >= HOLD_THRESHOLD && !active) {
                refreshCompatibleMags(player, gunItem.getGun());
                active = true;
            }
        }
    }

    public static void renderHUD(GuiGraphics graphics, Font font) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        float cx = screenWidth / 2.0f;
        float cy = screenHeight / 2.0f;

        int total = compatibleMags.size();

        // 1. Top Title Header
        graphics.drawCenteredString(font, Component.translatable("gui.gcr.mag_selector.title"), (int) cx, (int) cy - 90, 0xFFEEEEEE);

        if (total == 0) {
            // Empty State: Smooth rounded warning card
            SdfShader.begin(graphics.pose());
            SdfShader.drawRoundedRect(cx - 75.0f, cy - 18.0f, 150.0f, 36.0f, 8.0f, 0xD0181212, 0x88FF4444, 1.5f);
            SdfShader.end();

            graphics.drawCenteredString(font, Component.translatable("gui.gcr.mag_selector.no_mags"), (int) cx, (int) cy - 4, 0xFFFF6666);
            return;
        }

        // Clamp selectedIndex
        if (selectedIndex < 0) selectedIndex = 0;
        if (selectedIndex >= total) selectedIndex = total - 1;
        MagEntry selectedEntry = compatibleMags.get(selectedIndex);

        float orbitR = total <= 6 ? 64.0f : (total <= 10 ? 72.0f : 80.0f);
        int ammoColor = getAmmoColor(selectedEntry.ammo, selectedEntry.maxCapacity);
        float fillPct = selectedEntry.maxCapacity > 0 ? (float) selectedEntry.ammo / selectedEntry.maxCapacity : 0f;

        // Slot positions
        float[] slotX = new float[total];
        float[] slotY = new float[total];
        double[] slotAngle = new double[total];
        for (int i = 0; i < total; i++) {
            slotAngle[i] = -Math.PI / 2.0 + (2.0 * Math.PI * i / total);
            slotX[i] = cx + (float) (orbitR * Math.cos(slotAngle[i]));
            slotY[i] = cy + (float) (orbitR * Math.sin(slotAngle[i]));
        }

        // 2. Render SDF Canvas (Ultra-smooth rounded discs, rings, pills, arcs)
        SdfShader.begin(graphics.pose());

        // A. Subtle dark halo / backdrop ring behind entire wheel
        SdfShader.drawRing(cx, cy, orbitR, 38.0f, 0x55080C12);

        // B. Thin circular guide track
        SdfShader.drawRing(cx, cy, orbitR, 1.0f, 0x2A607890);

        // C. Center Hub: Smooth Circular Disc
        SdfShader.drawCircle(cx, cy, 26.0f, 0xEE0B1017, 0x88486078, 1.5f);

        // D. Center Circular Ammo Gauge (SDF Arc)
        if (fillPct > 0.005f) {
            float startRad = (float) (-Math.PI / 2.0);
            float endRad = (float) (startRad + 2.0 * Math.PI * fillPct);
            SdfShader.drawArc(cx, cy, 30.0f, 2.5f, startRad, endRad, ammoColor);
        } else {
            SdfShader.drawRing(cx, cy, 30.0f, 1.0f, 0x33445566);
        }

        // E. Radial Slots (Smooth Circular Plates)
        for (int i = 0; i < total; i++) {
            float sx = slotX[i];
            float sy = slotY[i];
            boolean isSelected = (i == selectedIndex);

            if (isSelected) {
                // Connecting indicator pip between center and selected slot
                float midX = cx + 0.62f * (sx - cx);
                float midY = cy + 0.62f * (sy - cy);
                SdfShader.drawCircle(midX, midY, 2.5f, 0xFF55FF55, 0, 0f);

                // Highlighted Slot Disc (Slightly larger, glowing green border)
                SdfShader.drawCircle(sx, sy, 18.0f, 0xF0182536, 0xFF55FF55, 2.0f);

                // Selected Ammo Badge Pill
                String ammoStr = String.valueOf(selectedEntry.ammo);
                int strW = font.width(ammoStr);
                float badgeW = Math.max(16.0f, strW + 8.0f);
                float badgeH = 10.0f;
                float badgeY = sy + (Math.sin(slotAngle[i]) < -0.3 ? -24.0f : 16.0f);
                float badgeX = sx - badgeW / 2.0f;
                SdfShader.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 4.5f, 0xEE000000, 0xAA55FF55, 1.0f);
            } else {
                // Unselected Slot Disc (Smooth circular dark plate)
                SdfShader.drawCircle(sx, sy, 14.0f, 0xCC0E141C, 0x445A6E84, 1.0f);
            }
        }

        SdfShader.end();

        // 3. Render Items and Text on top of SDF Canvas
        for (int i = 0; i < total; i++) {
            MagEntry entry = compatibleMags.get(i);
            int sx = Math.round(slotX[i]);
            int sy = Math.round(slotY[i]);
            boolean isSelected = (i == selectedIndex);

            // Item Icon
            graphics.renderItem(entry.stack, sx - 8, sy - 8);

            // Ammo number
            String slotAmmoStr = String.valueOf(entry.ammo);
            if (isSelected) {
                int badgeY = Math.round(sy + (Math.sin(slotAngle[i]) < -0.3 ? -24.0f : 16.0f));
                graphics.drawCenteredString(font, slotAmmoStr, sx, badgeY + 1, ammoColor);
            } else {
                int textY = Math.round(sy + (Math.sin(slotAngle[i]) < -0.3 ? -20.0f : 14.0f));
                graphics.drawCenteredString(font, slotAmmoStr, sx, textY, 0x88AAAAAA);
            }
        }

        // Center Hub Numbers: Large Ammo / Capacity
        String ammoText = String.valueOf(selectedEntry.ammo);
        graphics.drawCenteredString(font, ammoText, (int) cx, (int) cy - 11, ammoColor);
        graphics.fill((int) cx - 12, (int) cy, (int) cx + 12, (int) cy + 1, 0x55FFFFFF);
        String capText = "/" + selectedEntry.maxCapacity;
        graphics.drawCenteredString(font, capText, (int) cx, (int) cy + 3, 0xFFAAAAAA);

        // Selected Magazine Info Card (Below the wheel)
        String fullName = font.plainSubstrByWidth(selectedEntry.name, 190);
        graphics.drawCenteredString(font, fullName, (int) cx, (int) cy + 44, 0xFFFFFFFF);
        if (selectedEntry.caliber != null && !selectedEntry.caliber.isEmpty()) {
            graphics.drawCenteredString(font, selectedEntry.caliber, (int) cx, (int) cy + 54, 0xFF88AAFF);
        }

        // Bottom Hint
        graphics.drawCenteredString(font, Component.translatable("gui.gcr.mag_selector.hint"), (int) cx, (int) cy + 86, 0x88AAAAAA);
    }

    private static int getAmmoColor(int ammo, int maxCapacity) {
        if (ammo <= 0) {
            return 0xFFAAAAAA;
        } else if (ammo >= maxCapacity) {
            return 0xFF55FF55;
        } else if (ammo > maxCapacity / 2) {
            return 0xFFFFFF55;
        } else {
            return 0xFFFF7744;
        }
    }
}
