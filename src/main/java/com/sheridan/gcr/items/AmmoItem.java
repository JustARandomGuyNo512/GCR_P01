package com.sheridan.gcr.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmmoItem extends BaseItem {
    private static final Map<String, AmmoItem> BY_CALIBER = new HashMap<>();

    private final String caliber;

    public AmmoItem(String caliber, int stackSize) {
        super(new Item.Properties().stacksTo(stackSize));
        this.caliber = caliber;
        BY_CALIBER.put(caliber, this);
    }

    public String getCaliber() {
        return caliber;
    }

    public static AmmoItem getByCaliber(String caliber) {
        return BY_CALIBER.get(caliber);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.gcr.ammo.caliber", caliber).withStyle(ChatFormatting.GRAY));
    }
}
