package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.modules.IArmHandlerModular;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Handguard extends AttachmentModule implements ISlotProviderModular, IVoxelHandlerModule, IArmHandlerModular {
    private final ISlotProvider slotProvider;
    private final IVoxelHandler handler;
    private final AdditionalPropModifier additionalPropModifier;


    public Handguard(ResourceLocation id, float weight, float recoilControlInc, ISlotProvider slotProvider, IVoxelHandler handler, AdditionalPropModifier additionalPropModifier) {
        super(id, true, weight, Direction.NONE);
        this.slotProvider = slotProvider;
        this.handler = handler;
        this.additionalPropModifier = additionalPropModifier;
        defPropInc(BaseProperties.class, (p) -> p.recoilControl, recoilControlInc);
    }


    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

    @Override
    public IVoxelHandler getHandler() {
        return handler;
    }

    @Override
    public int getPriority(boolean rightArm) {
        return rightArm ? NONE_PRIORITY : HANDGUARD_PRIORITY;
    }

    @Override
    public @Nullable AdditionalPropModifier getModifier() {
        return additionalPropModifier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        AdditionalPropModifier modifier = getModifier();
        if (modifier != null) {
            modifier.appendHoverText(tooltipComponents);
        }
    }
}
