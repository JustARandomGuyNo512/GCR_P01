package com.sheridan.gcr.modularSys.modules.gunProperties;

import java.util.HashMap;
import java.util.Map;

public class PropertiesDummies {

    public static Map<Class<? extends IProperties>, IProperties> propertiesMap = new HashMap<>();

    public static void registerDummy(IProperties properties) {
        Class<? extends IProperties> clazz = properties.getClass();
        IProperties existing = propertiesMap.get(clazz);

        if (existing == null) {
            propertiesMap.put(clazz, properties);
            return;
        }

        String oldId = existing.getId();
        String newId = properties.getId();

        if (!oldId.equals(newId)) {
            throw new IllegalStateException(
                    "Duplicate IProperties class with different id! class="
                            + clazz.getName()
                            + ", oldId=" + oldId
                            + ", newId=" + newId
            );
        }
    }

    public static IProperties getDummy(Class<?> clazz) {
        return propertiesMap.get(clazz);
    }
}