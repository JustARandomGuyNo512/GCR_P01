package com.sheridan.gcr;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.animation.AnimationVariants;
import com.sheridan.gcr.client.animation.io.BedrockAnimationLoader;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.gltf.io.GltfModelLoader;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.modularSys.IModular;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModelRegistrationManager {
    // 存储需要延迟执行的编译任务
    private static final List<Runnable> DEFERRED_COMPILE_TASKS = new ArrayList<>();

    // 每个模块的模型“配方”：记录注册时用到的文件路径与工厂 Lambda，
    // 这样热重载单个模块时可以重新读文件、重跑同一段 Lambda 逻辑。
    private static final Map<IModular, ModelRecipe<?>> MODEL_RECIPES = new LinkedHashMap<>();

    /**
     * 一个模块模型的重建配方。
     *
     * @param modelPath gltf 资源位置
     * @param texturePath 网格纹理资源位置
     * @param modelFactory 注册时使用的工厂 Lambda（可重复执行，内部状态每次都重新创建）
     */
    public record ModelRecipe<T extends IModularModel>(
            ResourceLocation modelPath,
            ResourceLocation texturePath,
            Function<MeshModelData, T> modelFactory
    ) {
    }

    /**
     * 统一模型注册方法
     * @param registryKey 游戏内的注册实例键（如 GCR.M4A1）
     * @param gltfPath gltf文件相对路径
     * @param texturePath 纹理文件相对路径
     * @param immediateCompile 是否加入延迟编译队列
     * @param modelFactory 自定义实例化的Lambda编码参数
     */
    public static <T extends IModularModel> T registerModel(
            Object registryKey,
            String gltfPath,
            String texturePath,
            boolean immediateCompile,
            Function<MeshModelData, T> modelFactory
    ) {
        IModular module = (IModular) registryKey;
        ResourceLocation modelPath = GCR.RL("gcr", gltfPath);
        ResourceLocation texture = GCR.RL("gcr", texturePath);

        // 1. 加载模型资产
        MeshModelData meshModelData = GltfModelLoader.loadModel(modelPath);

        // 2. 运用用户自定义的 Lambda 逻辑生成模型实例
        T model = modelFactory.apply(meshModelData);

        // 3. 注册到系统的 ModuleModelRegister
        ModuleModelRegister.register(module, model);

        // 4. 记录重建配方，供热重载按模块重建
        MODEL_RECIPES.put(module, new ModelRecipe<>(modelPath, texture, modelFactory));

        // 5. 根据参数决定是否自动生成延迟编译任务
        if (immediateCompile) {
            DEFERRED_COMPILE_TASKS.add(() -> model.compile(RenderTypes.getMeshCutOut(texture)));
        }

        return model;
    }

    /** 该模块是否登记过可重建的模型配方。 */
    public static boolean hasRecipe(IModular module) {
        return MODEL_RECIPES.containsKey(module);
    }

    /** 模块模型用到的 gltf 资源位置；没有配方时返回 null。 */
    @Nullable
    public static ResourceLocation getModelPath(IModular module) {
        ModelRecipe<?> recipe = MODEL_RECIPES.get(module);
        return recipe == null ? null : recipe.modelPath();
    }

    /** 模块模型用到的纹理资源位置；没有配方时返回 null。 */
    @Nullable
    public static ResourceLocation getModelTexture(IModular module) {
        ModelRecipe<?> recipe = MODEL_RECIPES.get(module);
        return recipe == null ? null : recipe.texturePath();
    }

    /**
     * 按登记好的配方重新读取 gltf 并重新执行注册时的 Lambda。
     *
     * <p>只负责“重新装载”，不注册、不编译：调用方需要自行处理旧模型的释放与新模型的注册。</p>
     *
     * @return 新建的模型实例；该模块没有配方时返回 null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static IModularModel createModel(IModular module) {
        ModelRecipe<IModularModel> recipe = (ModelRecipe<IModularModel>) MODEL_RECIPES.get(module);
        if (recipe == null) {
            return null;
        }
        MeshModelData meshModelData = GltfModelLoader.loadModel(recipe.modelPath());
        return recipe.modelFactory().apply(meshModelData);
    }

    public static void addDeferredCompileTask(Runnable task) {
        DEFERRED_COMPILE_TASKS.add(task);
    }

    /**
     * 批量加载并自动注册动画的辅助方法
     */
    public static Map<String, AnimationDef> loadAndRegisterAnimations(String jsonPath, Map<String, String> mapping) {
        Map<String, AnimationDef> anims = BedrockAnimationLoader.loadAnimationCollection(GCR.RL("gcr", jsonPath), true);
        mapping.forEach((jsonKey, regKey) -> {
            AnimationDef def = anims.get(jsonKey);
            if (def != null) {
                AnimationRegister.register(GCR.RL(regKey), def);
            }
        });
        return anims;
    }

    /**
     * Loads a gun animation JSON and registers prefixed variants from the same
     * file ({@code shoot_one}, {@code mag_reload_empty_two}, …).
     */
    public static Map<String, AnimationDef> loadAndRegisterGunAnimations(
            String jsonPath, Map<String, String> mapping, String gunPrefix) {
        Map<String, AnimationDef> anims = loadAndRegisterAnimations(jsonPath, mapping);
        AnimationVariants.collectFromClips(gunPrefix, anims, mapping);
        return anims;
    }

    /**
     * 执行所有收集到的延迟编译任务
     */
    public static void compileAll() {
        for (Runnable task : DEFERRED_COMPILE_TASKS) {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
