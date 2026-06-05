package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.modularSys.IModular;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ModuleModelRegister {
    private static final Map<String, IModularModel> ID_TO_MODEL = new HashMap<>();
    private static final Map<IModular, IModularModel> MODULE_TO_MODEL = new HashMap<>();

    public static boolean register(IModular module, IModularModel model) {
        String id = module.getID();
        if (ID_TO_MODEL.containsKey(id) || MODULE_TO_MODEL.containsKey(module)) {
            return false;
        }
        ID_TO_MODEL.put(id, model);
        MODULE_TO_MODEL.put(module, model);
        return true;
    }

    public static void visitAll(Consumer<IModularModel> visitor) {
        ID_TO_MODEL.values().forEach(visitor);
    }

    public static IModularModel get(IModular module) {
        return MODULE_TO_MODEL.get(module);
    }

    public static IModularModel getByID(String id) {
        return ID_TO_MODEL.get(id);
    }

}
