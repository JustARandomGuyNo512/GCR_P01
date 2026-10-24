package com.sheridan.gcr.modularSys.builder;

public interface IWriteableAccessor extends IAccessor{

    void writeCustomParam(Unit unit, String key, int value);

    boolean setSlotHide(SlotInstance slot, boolean hide);

    void setOffset(Unit unit, float offset);
}
