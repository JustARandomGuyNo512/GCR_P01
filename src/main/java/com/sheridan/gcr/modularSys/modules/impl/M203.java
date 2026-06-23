package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.states.Str;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class M203 extends SubWeapon implements IVoxelHandlerModule, IArmHandlerModular, IStateModular {
    public static final String CHAMBER_EMPTY = "empty";
    public static final String CHAMBER_LOADED = "loaded";
    public static final String CHAMBER_FIRED = "fired";
    public static final Str CHAMBER_STATUS = new Str("chamber_status", CHAMBER_EMPTY);
    private final IVoxelHandler voxelHandler;
    private final AdditionalPropModifier modifier;

    public M203(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier) {
        super(id, true, weight, Direction.NONE);
        this.voxelHandler = voxelHandler;
        this.modifier = modifier;
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

    @Override
    public void onInitStates(CompoundTag states, String nodeId, String moduleId) {

    }

    @Override
    public void onUpdate(StatesUpdateContext context) {

    }
}
