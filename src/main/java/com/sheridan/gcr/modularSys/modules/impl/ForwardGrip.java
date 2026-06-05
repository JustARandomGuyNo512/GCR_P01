package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ForwardGrip extends UniqueModule implements IVoxelHandlerModule, IArmHandlerModular {
    private final MLokFitVoxelHandler handler;
    private final AdditionalPropModifier modifier;
    public ForwardGrip(ResourceLocation id, MLokFitVoxelHandler handler, float weight, AdditionalPropModifier modifier) {
        super(id, true, weight, Direction.LOWER);
        addTags("forward_grip", "lower", "on_rail");
        this.handler = handler;
        this.modifier = modifier;
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        accessor.getBelongsTo(thisUnit).ifPresent(belongsTo -> {
            if (belongsTo.getSlot().hasTag("m_lok_rail")) {
                accessor.writeCustomParam(thisUnit, IVoxelHandler.VOXEL_INDEX_PARAM_KEY, 1);
            }
        });
    }

    @Override
    public IVoxelHandler getHandler() {
        return handler;
    }

    @Override
    public int getPriority(boolean rightArm) {
        return rightArm ? IArmHandlerModular.NONE_PRIORITY : IArmHandlerModular.GRIP_PRIORITY;
    }

    @Override
    public @Nullable AdditionalPropModifier getModifier() {
        return modifier;
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
