package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.IAccessor;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IArmHandlerModular;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.SplitARHandguardVoxelHandler;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SplitARHandguard extends AttachmentModule implements IVoxelHandlerModule, IArmHandlerModular {
    public static final String HIDE_LOWER_PART_PARAM_KEY = "hide_lower_part";
    private final IVoxelHandler voxelHandler;

    private final AdditionalPropModifier additionalPropModifier;

    public SplitARHandguard(ResourceLocation id, float weight, float recoilControlInc,
                            SplitARHandguardVoxelHandler handler, AdditionalPropModifier additionalPropModifier) {
        super(id, true, weight, Direction.NONE);
        addTags("two_part_handguard");
        voxelHandler = handler;
        defPropInc(BaseProperties.class, p -> p.recoilControl, recoilControlInc);
        this.additionalPropModifier = additionalPropModifier;
    }


    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        findFirstUnderBarrel(accessor)
                .ifPresent(underBarrel ->
                        accessor.writeCustomParam(thisUnit, HIDE_LOWER_PART_PARAM_KEY, 1));
    }

    public static Optional<Unit> findFirstUnderBarrel(IAccessor accessor) {
        List<Unit> barrel = accessor.getSlotChildren(accessor.root(), "BARREL");
        if (barrel.isEmpty()) {
            return Optional.empty();
        }
        //定位第一个下挂枪管配件
        for (Unit unit : barrel) {
            List<Unit> children = accessor.getSlotChildren(unit, "UNDER_BARREL");
            if (!children.isEmpty()) {
                return Optional.of(children.getFirst());
            }
        }
        return Optional.empty();
    }


    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
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
