package com.sheridan.gcr.modularSys.modules.gunProperties;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PropertiesAccessor {
    private final CompoundTag tag;
    private final Map<Class<? extends IProperties>, IProperties> moduleRegistry;

    private PropertiesAccessor(CompoundTag tag, Map<Class<? extends IProperties>, IProperties> registry) {
        this.tag = tag;
        this.moduleRegistry = registry;

    }

    public static PropertiesAccessor of(CompoundTag tag, Map<String, IProperties> registry) {
        Map<Class<? extends IProperties>, IProperties> mapped = new HashMap<>();

        for (IProperties prop : registry.values()) {
            mapped.put(prop.getClass(), prop);
        }

        return new PropertiesAccessor(tag, mapped);
    }

    public <T extends IProperties> void using(Class<T> clazz, Consumer<T> callback) {
        IProperties iProperties = moduleRegistry.get(clazz);
        if (clazz.isInstance(iProperties)) {
            T p = clazz.cast(iProperties);
            CompoundTag compound = tag.getCompound(p.getId());
            p.bindProp(compound);
            callback.accept(p);
            p.unbindProp();
            tag.put(p.getId(), compound);
        }
    }

}