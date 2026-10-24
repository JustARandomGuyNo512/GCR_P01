package com.sheridan.gcr.modularSys;

import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.slot.ISlot;
import com.sheridan.gcr.modularSys.util.PivotMap;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SlotProvider implements ISlotProvider{
    private final ResourceLocation pivotMapPath;
    private PivotMap pivotMap;
    private final List<ISlot> slots;
    private final Set<String> slotNames;

    public SlotProvider(ResourceLocation pivotMapPath) {
        this.pivotMapPath = pivotMapPath;
        this.slots = new ArrayList<>();
        this.slotNames = new HashSet<>();
    }

    public SlotProvider(ResourceLocation pivotMapPath, List<ISlot> slots) {
        this.pivotMapPath = pivotMapPath;
        this.slots = slots;
        this.slotNames = new HashSet<>();
    }

    @Override
    public SlotProvider addSlot(ISlot slot) {
        if (this.hasSlot(slot.getName())) {
            return this;
        }
        this.slots.add(slot);
        this.slotNames.add(slot.getName());
        return this;
    }

    @Override
    public boolean hasSlot(String slotName) {
        return slotNames.contains(slotName);
    }


    @Override
    public ResourceLocation getAssetsPath() {
        return pivotMapPath;
    }

    @Override
    public void setPivotMap(PivotMap pivotMap) {
        this.pivotMap = pivotMap;
    }

    @Override
    public PivotMap getPivotMap() {
        return this.pivotMap;
    }

    @Override
    public List<ISlot> getSlots() {
        return slots;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }
}
