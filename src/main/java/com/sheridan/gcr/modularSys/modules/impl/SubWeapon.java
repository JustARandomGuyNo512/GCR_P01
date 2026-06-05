package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.modules.IInteractiveModular;
import com.sheridan.gcr.modularSys.modules.UniqueModule;
import net.minecraft.resources.ResourceLocation;

public class SubWeapon extends UniqueModule implements IInteractiveModular {
    public SubWeapon(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
    }
}
