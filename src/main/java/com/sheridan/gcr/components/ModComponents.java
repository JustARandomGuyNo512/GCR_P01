package com.sheridan.gcr.components;

import com.sheridan.gcr.GCR;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GCR.MODID);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
