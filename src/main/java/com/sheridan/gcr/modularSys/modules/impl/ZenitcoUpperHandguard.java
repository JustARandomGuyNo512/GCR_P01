package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.IAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.builder.ValidateResult;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class ZenitcoUpperHandguard extends SlotProviderVoxelModule{
    static List<String> allSuitable = null;

    public ZenitcoUpperHandguard(ResourceLocation id, boolean fixedPosition, float weight, Direction direction, ISlotProvider slotProvider, IVoxelHandler handler) {
        super(id, fixedPosition, weight, direction, slotProvider, handler);
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();
        if (allSuitable != null) {
            return;
        }
        allSuitable = new ArrayList<>();
        Collection<IModular> values = ModuleRegister.all().values();
        for (IModular m : values) {
            if (m.hasTag("zenitco")) {
                allSuitable.add(m.getID());
            }
        }
    }

    @Override
    public void validate(IAccessor accessor, Unit thisUnit, ValidateResult result) {
        super.validate(accessor, thisUnit, result);
        accessor.getSlot(accessor.root(), "HANDGUARD_LOWER").ifPresent(slot -> {
            List<Unit> slotChildren = accessor.getSlotChildren(slot);
            boolean found = false;
            for (Unit unit : slotChildren) {
                if (unit.hasTag("zenitco")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String string = Component.translatable("validate.result.requires").getString();
                String thisName = Component.translatable(getID()).getString();
                StringBuilder list = new StringBuilder();
                for (String s : allSuitable) {
                    list.append(Component.translatable(s).getString()).append(", ");
                }
                String msg = string.replace("$name", thisName).replace("$list", list.toString());
                result.recordError(msg);
            }
        });
    }
}
