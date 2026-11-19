package com.sheridan.gcr.client;

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
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        int total = compatibleMags.size();

        // 1. Top Title Header
        graphics.drawCenteredString(font, Component.translatable("gui.gcr.mag_selector.title"), cx, cy - 88, 0xFFEEEEEE);

        if (total == 0) {
            // Empty State: Sleek warning plate
            int cardW = 140;
            int cardH = 34;
            graphics.fill(cx - cardW / 2, cy - cardH / 2, cx + cardW / 2, cy + cardH / 2, 0xD0181212);
            graphics.renderOutline(cx - cardW / 2, cy - cardH / 2, cardW, cardH, 0x88FF4444);
            graphics.drawCenteredString(font, Component.translatable("gui.gcr.mag_selector.no_mags"), cx, cy - 4, 0xFFFF6666);
            return;
        }

        // Clamp selectedIndex
        if (selectedIndex < 0) selectedIndex = 0;
        if (selectedIndex >= total) selectedIndex = total - 1;
        MagEntry selectedEntry = compatibleMags.get(selectedIndex);

        // 2. Orbit Radius calculation
        int orbitR = total <= 6 ? 62 : (total <= 10 ? 70 : 78);

        // 3. Render Center Hub
        int hubW = 46;
        int hubH = 46;
        int hubX = cx - hubW / 2;
        int hubY = cy - hubH / 2;

        // Chamfered center plate
        graphics.fill(hubX + 2, hubY, hubX + hubW - 2, hubY + hubH, 0xDD0E141D);
        graphics.fill(hubX, hubY + 2, hubX + hubW, hubY + hubH - 2, 0xDD0E141D);
        graphics.renderOutline(hubX + 1, hubY + 1, hubW - 2, hubH - 2, 0x77556677);

        // Circular ammo gauge around the center hub
        int ammoColor = getAmmoColor(selectedEntry.ammo, selectedEntry.maxCapacity);
        float fillPct = selectedEntry.maxCapacity > 0 ? (float) selectedEntry.ammo / selectedEntry.maxCapacity : 0f;
        int gaugeR = 26;
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a - 90);
            int rx = cx + (int) Math.round(gaugeR * Math.cos(rad));
            int ry = cy + (int) Math.round(gaugeR * Math.sin(rad));
            int dotColor = (a / 360.0f <= fillPct) ? ammoColor : 0x33445566;
            graphics.fill(rx, ry, rx + 1, ry + 1, dotColor);
        }

        // Center Text: Ammo / Capacity
        String ammoText = String.valueOf(selectedEntry.ammo);
        graphics.drawCenteredString(font, ammoText, cx, cy - 11, ammoColor);
        graphics.fill(cx - 12, cy, cx + 12, cy + 1, 0x55FFFFFF);
        String capText = "/" + selectedEntry.maxCapacity;
        graphics.drawCenteredString(font, capText, cx, cy + 3, 0xFFAAAAAA);

        // 4. Render Radial Slots
        for (int i = 0; i < total; i++) {
            MagEntry entry = compatibleMags.get(i);
            boolean isSelected = (i == selectedIndex);

            double angle = -Math.PI / 2.0 + (2.0 * Math.PI * i / total);
            int sx = cx + (int) Math.round(orbitR * Math.cos(angle));
            int sy = cy + (int) Math.round(orbitR * Math.sin(angle));

            if (isSelected) {
                // Connecting dots from center hub towards selected slot
                for (double t = 0.52; t <= 0.76; t += 0.12) {
                    int dx = (int) Math.round(cx + t * (sx - cx));
                    int dy = (int) Math.round(cy + t * (sy - cy));
                    graphics.fill(dx - 1, dy - 1, dx + 2, dy + 2, 0xFF55FF55);
                }

                // Selected Slot (Larger 32x32, highlighted)
                int cardSize = 32;
                int cardX = sx - cardSize / 2;
                int cardY = sy - cardSize / 2;

                graphics.fill(cardX + 2, cardY, cardX + cardSize - 2, cardY + cardSize, 0xEE1E2838);
                graphics.fill(cardX, cardY + 2, cardX + cardSize, cardY + cardSize - 2, 0xEE1E2838);
                graphics.renderOutline(cardX + 1, cardY + 1, cardSize - 2, cardSize - 2, 0xFF55FF55);

                graphics.renderItem(entry.stack, sx - 8, sy - 8);

                // Ammo badge pill
                String slotAmmoStr = String.valueOf(entry.ammo);
                int strW = font.width(slotAmmoStr);
                int badgeW = Math.max(14, strW + 6);
                int badgeH = 9;
                int badgeY = sy + (Math.sin(angle) < -0.3 ? -22 : 15);
                int badgeX = sx - badgeW / 2;

                graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xEE000000);
                graphics.renderOutline(badgeX, badgeY, badgeW, badgeH, 0x8855FF55);
                graphics.drawCenteredString(font, slotAmmoStr, sx, badgeY + 1, ammoColor);

            } else {
                // Unselected Slot (Standard 24x24)
                int cardSize = 24;
                int cardX = sx - cardSize / 2;
                int cardY = sy - cardSize / 2;

                graphics.fill(cardX + 2, cardY, cardX + cardSize - 2, cardY + cardSize, 0xAA10151C);
                graphics.fill(cardX, cardY + 2, cardX + cardSize, cardY + cardSize - 2, 0xAA10151C);
                graphics.renderOutline(cardX + 1, cardY + 1, cardSize - 2, cardSize - 2, 0x44607080);

                graphics.renderItem(entry.stack, sx - 8, sy - 8);

                // Subtle ammo text
                String slotAmmoStr = String.valueOf(entry.ammo);
                int textY = sy + (Math.sin(angle) < -0.3 ? -18 : 13);
                graphics.drawCenteredString(font, slotAmmoStr, sx, textY, 0x88AAAAAA);
            }
        }

        // 5. Selected Magazine Info (Below the wheel)
        String fullName = font.plainSubstrByWidth(selectedEntry.name, 180);
        graphics.drawCenteredString(font, fullName, cx, cy + 42, 0xFFFFFFFF);
        if (selectedEntry.caliber != null && !selectedEntry.caliber.isEmpty()) {
            graphics.drawCenteredString(font, selectedEntry.caliber, cx, cy + 52, 0xFF88AAFF);
        }

        // 6. Bottom Hint
        graphics.drawCenteredString(font, Component.translatable("gui.gcr.mag_selector.hint"), cx, cy + 86, 0x88AAAAAA);
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
