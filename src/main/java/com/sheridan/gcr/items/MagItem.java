package com.sheridan.gcr.items;

import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.Mag;
import com.sheridan.gcr.network.c2s.MagLoadPacket;
import com.sheridan.gcr.client.MagClientBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MagItem extends ModuleItem<Mag> {
    public MagItem(Mag module) {
        super(module);
    }

    public static int getAmmoLeft(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(IAmmoSource.AMMO_LEFT.name)) {
            return tag.getInt(IAmmoSource.AMMO_LEFT.name);
        }
        return 0;
    }

    public static void setAmmoLeft(ItemStack stack, int ammo) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(IAmmoSource.AMMO_LEFT.name, Math.max(0, ammo));
        });
    }

    public String getCaliber() {
        for (String tag : getModule().getTags()) {
            if (tag.contains("x")) {
                return tag;
            }
        }
        return "Unknown";
    }

    public boolean acceptsAmmo(AmmoItem ammoItem) {
        return getModule().hasTag(ammoItem.getCaliber());
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        int ammo = getAmmoLeft(stack);
        int max = getModule().getMaxCapacity();
        if (max <= 0) return 0;
        return Math.round(13.0f * (float) ammo / (float) max);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        int ammo = getAmmoLeft(stack);
        int max = getModule().getMaxCapacity();
        float f = max <= 0 ? 0.0f : Math.max(0.0f, (float) ammo / (float) max);
        return Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.gcr.mag.caliber", getCaliber()).withStyle(ChatFormatting.GRAY));
        int ammo = getAmmoLeft(stack);
        int max = getModule().getMaxCapacity();
        tooltipComponents.add(Component.translatable("tooltip.gcr.mag.ammo", ammo, max).withStyle(ammo > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess access) {
        int currentAmmo = getAmmoLeft(stack);
        int maxCap = getModule().getMaxCapacity();

        if (other.getItem() instanceof AmmoItem ammoItem && acceptsAmmo(ammoItem)) {
            if (currentAmmo >= maxCap) {
                return false;
            }

            // Return ammo to player inventory and clear cursor
            access.set(ItemStack.EMPTY);
            if (player.containerMenu != null) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            ItemStack ammoToReturn = other.copy();
            if (!player.getInventory().add(ammoToReturn)) {
                player.drop(ammoToReturn, false);
            }

            // Equip magazine in selected hotbar slot
            int selectedHotbar = player.getInventory().selected;
            int containerSlot = slot.getContainerSlot();
            ItemStack magStack = stack.copy();
            slot.set(ItemStack.EMPTY);

            ItemStack currentlyHeld = player.getInventory().getItem(selectedHotbar);
            if (!currentlyHeld.isEmpty()) {
                slot.set(currentlyHeld);
            }
            player.getInventory().setItem(selectedHotbar, magStack);

            player.closeContainer();

            if (FMLEnvironment.dist == Dist.CLIENT) {
                MagClientBridge.startLoadingFromInventory(getCaliber());
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new MagLoadPacket(MagLoadPacket.ACTION_START, getCaliber()));
            }

            return true;
        }

        if (other.isEmpty() && action == ClickAction.SECONDARY && currentAmmo > 0) {
            AmmoItem matchingAmmo = AmmoItem.getByCaliber(getCaliber());
            if (matchingAmmo != null) {
                int unloadCount = 1;
                setAmmoLeft(stack, currentAmmo - unloadCount);
                access.set(new ItemStack(matchingAmmo, unloadCount));
                player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8f, 1.2f);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack stack, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player) {
        ItemStack slotItem = slot.getItem();
        int currentAmmo = getAmmoLeft(stack);
        int maxCap = getModule().getMaxCapacity();

        if (slotItem.getItem() instanceof AmmoItem ammoItem && acceptsAmmo(ammoItem)) {
            if (currentAmmo >= maxCap) {
                return false;
            }

            // stack is the magazine on cursor
            if (player.containerMenu != null) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }

            int selectedHotbar = player.getInventory().selected;
            ItemStack magStack = stack.copy();

            ItemStack currentlyHeld = player.getInventory().getItem(selectedHotbar);
            if (!currentlyHeld.isEmpty()) {
                if (!player.getInventory().add(currentlyHeld)) {
                    player.drop(currentlyHeld, false);
                }
            }
            player.getInventory().setItem(selectedHotbar, magStack);

            player.closeContainer();

            if (FMLEnvironment.dist == Dist.CLIENT) {
                MagClientBridge.startLoadingFromInventory(getCaliber());
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new MagLoadPacket(MagLoadPacket.ACTION_START, getCaliber()));
            }

            return true;
        }

        if (action == ClickAction.SECONDARY && currentAmmo > 0) {
            AmmoItem matchingAmmo = AmmoItem.getByCaliber(getCaliber());
            if (matchingAmmo != null) {
                if (slotItem.isEmpty()) {
                    setAmmoLeft(stack, currentAmmo - 1);
                    slot.set(new ItemStack(matchingAmmo, 1));
                    player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8f, 1.2f);
                    return true;
                } else if (slotItem.getItem() == matchingAmmo && slotItem.getCount() < slotItem.getMaxStackSize()) {
                    setAmmoLeft(stack, currentAmmo - 1);
                    slotItem.grow(1);
                    player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8f, 1.2f);
                    return true;
                }
            }
        }

        return false;
    }
}
