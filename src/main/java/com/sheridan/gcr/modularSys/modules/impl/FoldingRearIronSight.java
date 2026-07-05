package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.FoldingIronSightVoxelHandler;
import com.sheridan.gcr.modularSys.modules.ISight;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class FoldingRearIronSight extends Sight{
    public FoldingRearIronSight(ResourceLocation id, FoldingIronSightVoxelHandler voxelHandler, float weight, boolean fixedPosition, float adsSpeedModifier) {
        super(id, voxelHandler, weight, fixedPosition, adsSpeedModifier);
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        handleFolding(accessor, thisUnit);
    }

    public static void handleFolding(IWriteableAccessor accessor, Unit thisUnit) {
        accessor.filter(
                ((Predicate<Unit>)unit -> {
                    if (unit.getModule() instanceof ISight sight) {
                        AtomicInteger sightPriority = new AtomicInteger(sight.getSightPriority(unit));
                        accessor.getParent(unit).ifPresent(parent -> {
                            if (parent.getModule() instanceof CantedRail) {
                                sightPriority.set(ISight.SIDE);
                            }
                        });
                        return sightPriority.get() > ISight.IRON_SIGHT;
                    }
                    return false;
                }).and(unit -> !Objects.equals(unit.getModuleId(), thisUnit.getModuleId()))
        ).ifPresent(units -> accessor.writeCustomParam(thisUnit, FoldingIronSightVoxelHandler.FOLD_SIGHT_PARAM_KEY, 1));
    }

    @Override
    public int defaultSightPriority(Unit unit) {
        int customParam = unit.getCustomParam(FoldingIronSightVoxelHandler.FOLD_SIGHT_PARAM_KEY);
        return customParam == -1 ? IRON_SIGHT : IGNORE;
    }
}
