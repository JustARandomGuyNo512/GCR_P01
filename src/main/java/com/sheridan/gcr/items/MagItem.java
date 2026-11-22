package com.sheridan.gcr.items;

import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.Mag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        return false;
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
            return true;
        }

        if (other.isEmpty() && action == ClickAction.SECONDARY && currentAmmo > 0) {
            AmmoItem matchingAmmo = AmmoItem.getByCaliber(getCaliber());
            if (matchingAmmo != null) {
                int unloadCount = 1;
                setAmmoLeft(stack, currentAmmo - unloadCount);
                access.set(new ItemStack(matchingAmmo, unloadCount));
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
            return true;
        }

        if (action == ClickAction.SECONDARY && currentAmmo > 0) {
            AmmoItem matchingAmmo = AmmoItem.getByCaliber(getCaliber());
            if (matchingAmmo != null) {
                if (slotItem.isEmpty()) {
                    setAmmoLeft(stack, currentAmmo - 1);
                    slot.set(new ItemStack(matchingAmmo, 1));
                    return true;
                } else if (slotItem.getItem() == matchingAmmo && slotItem.getCount() < slotItem.getMaxStackSize()) {
                    setAmmoLeft(stack, currentAmmo - 1);
                    slotItem.grow(1);
                    return true;
                }
            }
        }

        return false;
    }
}
