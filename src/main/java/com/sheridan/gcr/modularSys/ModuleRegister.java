package com.sheridan.gcr.modularSys;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ModuleRegister {
    private static Map<String, IModular> MODULES = new HashMap<>();
    private static boolean frozen = false;

    public static boolean register(String id, IModular modular) {
        if (frozen) {
            throw new IllegalStateException("ModuleRegister already finalized");
        }
        if (MODULES.containsKey(id)) {
            return false;
        }
        MODULES.put(id, modular);
        return true;
    }

    public static void finalizeRegister() {
        if (frozen) {
            return;
        }
        MODULES = Map.copyOf(MODULES);
        frozen = true;
    }

    public static IModular remove(String id) {
        if (frozen) {
            throw new IllegalStateException("ModuleRegister already finalized");
        }
        return MODULES.remove(id);
    }

    @Nullable
    public static IModular get(String id) {
        return MODULES.get(id);
    }

    @Nullable
    public static <T> T get(String id, Class<T> clazz) {
        IModular iModular = get(id);
        return clazz.isInstance(iModular) ? clazz.cast(iModular) : null;
    }

    public static boolean has(String id) {
        return MODULES.containsKey(id);
    }

    public static Map<String, IModular> all() {
        return MODULES;
    }
}

