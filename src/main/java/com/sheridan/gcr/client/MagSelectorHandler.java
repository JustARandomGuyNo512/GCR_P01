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
    public static final int HOLD_THRESHOLD = 5;
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

        public MagEntry(int slot, ItemStack stack, MagItem item, int ammo, int maxCapacity, String name) {
            this.slot = slot;
            this.stack = stack;
            this.item = item;
            this.ammo = ammo;
            this.maxCapacity = maxCapacity;
            this.name = name;
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
                    compatibleMags.add(new MagEntry(i, stack, magItem, ammo, maxCap, name));
                }
            }
        }
        compatibleMags.sort((a, b) -> Integer.compare(b.ammo, a.ammo));
        selectedIndex = 0;
    }

    public static boolean onMouseScroll(double deltaY) {
        if (!active || compatibleMags.isEmpty()) {
            return false;
        }
        int prev = selectedIndex;
        if (deltaY > 0) {
            selectedIndex--;
            if (selectedIndex < 0) {
                selectedIndex = compatibleMags.size() - 1;
            }
        } else if (deltaY < 0) {
            selectedIndex++;
            if (selectedIndex >= compatibleMags.size()) {
                selectedIndex = 0;
            }
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

        int cardWidth = 148;
        int cardHeight = 26;
        int cardMargin = 4;
        int maxVisible = 5;

        int total = compatibleMags.size();
        if (total == 0) {
            int totalH = 36;
            int startX = screenWidth / 2 + 50;
            int startY = (screenHeight - totalH) / 2;

            graphics.drawString(font, "МАГАЗИНЫ", startX + 2, startY - 12, 0xFFAAAAAA, false);
            graphics.fill(startX, startY, startX + cardWidth, startY + totalH, 0xD0181212);
            graphics.renderOutline(startX, startY, cardWidth, totalH, 0x88FF4444);
            graphics.drawString(font, "НЕТ ПОДХОДЯЩИХ", startX + 10, startY + 8, 0xFFFF6666, false);
            graphics.drawString(font, "МАГАЗИНОВ", startX + 10, startY + 20, 0xFFAAAAAA, false);
            return;
        }

        int startIdx = 0;
        if (total > maxVisible) {
            startIdx = selectedIndex - maxVisible / 2;
            if (startIdx < 0) startIdx = 0;
            if (startIdx + maxVisible > total) startIdx = total - maxVisible;
        }
        int visibleCount = Math.min(total, maxVisible);
        int listHeight = visibleCount * (cardHeight + cardMargin) - cardMargin;

        int startX = screenWidth / 2 + 50;
        int startY = (screenHeight - listHeight) / 2;

        graphics.drawString(font, "МАГАЗИНЫ", startX + 2, startY - 13, 0xFFAAAAAA, false);

        if (startIdx > 0) {
            graphics.drawCenteredString(font, "▲", startX + cardWidth / 2, startY - 9, 0xFFFFFFFF);
        }

        for (int vi = 0; vi < visibleCount; vi++) {
            int index = startIdx + vi;
            MagEntry entry = compatibleMags.get(index);
            boolean isSelected = (index == selectedIndex);

            int cy = startY + vi * (cardHeight + cardMargin);

            if (isSelected) {
                graphics.fill(startX, cy, startX + cardWidth, cy + cardHeight, 0xDD222832);
                graphics.renderOutline(startX, cy, cardWidth, cardHeight, 0xFFFFFFFF);
                graphics.fill(startX, cy, startX + 3, cy + cardHeight, 0xFF55FF55);
            } else {
                graphics.fill(startX, cy, startX + cardWidth, cy + cardHeight, 0x99101418);
                graphics.renderOutline(startX, cy, cardWidth, cardHeight, 0x44888888);
            }

            graphics.renderItem(entry.stack, startX + 6, cy + 5);

            String name = font.plainSubstrByWidth(entry.name, cardWidth - 32);
            int nameColor = isSelected ? 0xFFFFFFFF : 0xFFCCCCCC;
            graphics.drawString(font, name, startX + 26, cy + 4, nameColor, false);

            String ammoStr = entry.ammo + "/" + entry.maxCapacity;
            int ammoColor;
            if (entry.ammo == 0) {
                ammoColor = 0xFFAAAAAA;
            } else if (entry.ammo >= entry.maxCapacity) {
                ammoColor = 0xFF55FF55;
            } else if (entry.ammo > entry.maxCapacity / 2) {
                ammoColor = 0xFFFFFF55;
            } else {
                ammoColor = 0xFFFF7744;
            }
            graphics.drawString(font, ammoStr, startX + 26, cy + 14, ammoColor, false);

            int barW = 48;
            int barH = 3;
            int barX = startX + cardWidth - barW - 6;
            int barY = cy + 16;
            graphics.fill(barX, barY, barX + barW, barY + barH, 0x88000000);
            int fillW = entry.maxCapacity > 0 ? (int) ((float) entry.ammo / entry.maxCapacity * barW) : 0;
            fillW = Math.max(0, Math.min(barW, fillW));
            if (fillW > 0) {
                graphics.fill(barX, barY, barX + fillW, barY + barH, ammoColor);
            }
        }

        if (startIdx + visibleCount < total) {
            int downY = startY + listHeight + 1;
            graphics.drawCenteredString(font, "▼", startX + cardWidth / 2, downY, 0xFFFFFFFF);
        }

        int hintY = startY + listHeight + (startIdx + visibleCount < total ? 10 : 4);
        graphics.drawString(font, "Отпустите R для смены", startX + 2, hintY, 0x88AAAAAA, false);
    }
}
