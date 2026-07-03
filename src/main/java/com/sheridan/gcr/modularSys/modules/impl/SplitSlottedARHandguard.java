package com.sheridan.gcr.modularSys.modules.impl;

import com.google.common.collect.ImmutableSet;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.modules.SplitARHandguardVoxelHandler;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SplitSlottedARHandguard extends SplitARHandguard implements ISlotProviderModular {
    private final ISlotProvider slotProvider;
    private final Set<String> lowerSlots;

    public SplitSlottedARHandguard(ResourceLocation id, ISlotProvider slotProvider,
                                   float weight, float recoilControlInc,
                                   SplitARHandguardVoxelHandler handler,
                                   AdditionalPropModifier additionalPropModifier,
                                   String... lowerSlotNames) {
        super(id, weight, recoilControlInc, handler, additionalPropModifier);
        this.slotProvider = slotProvider;
        lowerSlots = ImmutableSet.copyOf(lowerSlotNames);
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        findFirstUnderBarrel(accessor).ifPresent(underBarrel ->
                accessor.filterSlots(thisUnit, s -> lowerSlots.contains(s.slotName()))
                        .ifPresentOrElse(slots -> {
                            boolean hide = true;
                            for (SlotInstance slotInstance : slots) {
                                if (!accessor.getSlotChildren(slotInstance).isEmpty()) {
                                    hide = false;
                                    break;
                                }
                            }
                            if (hide) {
                                accessor.writeCustomParam(thisUnit, HIDE_LOWER_PART_PARAM_KEY, 1);
                                slots.forEach(s -> accessor.setSlotHide(s, true));
                            }
                            },
                                () -> accessor.writeCustomParam(thisUnit, HIDE_LOWER_PART_PARAM_KEY, 1))
        );
    }

    @Override
    public void validate(IAccessor accessor, Unit thisUnit, ValidateResult result) {
        findFirstUnderBarrel(accessor).ifPresent(underBarrel -> {
            //对于自身的lower part配件，如果不为空，则产生冲突
            List<SlotInstance> slots = accessor.filterSlots(thisUnit, slot -> lowerSlots.contains(slot.slotName())).orElse(List.of());
            for (SlotInstance slotInstance : slots) {
                if (!accessor.slotEmpty(slotInstance)) {
                    result.recordError(underBarrel, "Can not fit under-barrel module because there has been a module mounted at lower part of this handguard");
                    return;
                }
            }
        });
    }
}
