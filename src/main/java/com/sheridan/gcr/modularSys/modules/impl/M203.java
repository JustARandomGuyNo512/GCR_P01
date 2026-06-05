package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.IArmHandlerModular;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class M203 extends SubWeapon implements IAmmoSource, IVoxelHandlerModule, IArmHandlerModular {
    private final IVoxelHandler voxelHandler;
    private final AdditionalPropModifier modifier;

    public M203(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier) {
        super(id, true, weight, Direction.NONE);
        this.voxelHandler = voxelHandler;
        this.modifier = modifier;
    }

    @Override
    public void setAmmoLeft(int ammoLeft, CompoundTag states) {
        ammoLeft = Mth.clamp(ammoLeft, 0, getMaxCapacity());
        AMMO_LEFT.set(ammoLeft, states);
    }

    @Override
    public int getAmmoLeft(CompoundTag states) {
        return AMMO_LEFT.get(states);
    }

    @Override
    public int getMaxCapacity() {
        return 1;
    }

    @Override
    public int getPriority() {
        return IAmmoSourceView.NONE_PRIORITY;
    }


    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }

    @Override
    public int getPriority(boolean rightArm) {
        return rightArm ? IArmHandlerModular.NONE_PRIORITY : IArmHandlerModular.SUB_WEAPON_PRIORITY;
    }

    @Override
    public @Nullable AdditionalPropModifier getModifier() {
        return modifier;
    }
}
