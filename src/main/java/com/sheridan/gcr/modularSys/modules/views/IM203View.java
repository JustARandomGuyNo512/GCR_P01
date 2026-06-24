package com.sheridan.gcr.modularSys.modules.views;

import com.sheridan.gcr.modularSys.modules.states.Str;
import net.minecraft.nbt.CompoundTag;

public interface IM203View extends IStateView{
    String CHAMBER_EMPTY = "empty";
    String CHAMBER_LOADED = "loaded";
    String CHAMBER_FIRED = "fired";
    Str CHAMBER_STATUS = new Str("chamber_status", CHAMBER_EMPTY);
    String getChamberStatus(CompoundTag states);
}
