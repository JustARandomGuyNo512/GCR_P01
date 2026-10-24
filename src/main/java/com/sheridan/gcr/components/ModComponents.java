package com.sheridan.gcr.components;

import net.minecraftforge.eventbus.api.IEventBus;
/** Upstream DataComponentType registry is empty. NBT is used in Forge 1.20.1. */
public final class ModComponents {
    private ModComponents() {}
    public static void register(IEventBus bus) { }
}
