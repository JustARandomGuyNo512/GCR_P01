package com.sheridan.gcr.modularSys;

import com.sheridan.gcr.IJsonSync;
import com.sheridan.gcr.modularSys.slot.ISlot;
import com.sheridan.gcr.modularSys.util.PivotMap;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface ISlotProvider extends IJsonSync {
    ResourceLocation getAssetsPath();
    void setPivotMap(PivotMap pivotMap);
    PivotMap getPivotMap();
    List<ISlot> getSlots();
    ISlotProvider addSlot(ISlot slotDef);
    boolean hasSlot(String slotName);
}
