package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.modularSys.IModular;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

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

    /**
     * 移除某个模块的模型并返回被移除的实例，供热重载释放旧模型。
     *
     * @return 该模块原本登记的模型；没有登记过则返回 null
     */
    @Nullable
    public static IModularModel remove(IModular module) {
        IModularModel removed = MODULE_TO_MODEL.remove(module);
        if (removed != null) {
            ID_TO_MODEL.remove(module.getID(), removed);
        }
        return removed;
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
