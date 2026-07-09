package com.sheridan.gcr;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.AnimationRegister;
import com.sheridan.gcr.client.animation.io.BedrockAnimationLoader;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.gltf.io.GltfModelLoader;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.ModularModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.modularSys.IModular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModelRegistrationManager {
    // 存储需要延迟执行的编译任务
    private static final List<Runnable> DEFERRED_COMPILE_TASKS = new ArrayList<>();

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
        // 1. 加载模型资产
        MeshModelData meshModelData = GltfModelLoader.loadModel(GCR.RL("gcr", gltfPath));

        // 2. 运用用户自定义的 Lambda 逻辑生成模型实例
        T model = modelFactory.apply(meshModelData);

        // 3. 注册到系统的 ModuleModelRegister
        ModuleModelRegister.register((IModular) registryKey, model);

        // 4. 根据参数决定是否自动生成延迟编译任务
        if (immediateCompile) {
            DEFERRED_COMPILE_TASKS.add(() -> {
                model.compile(RenderTypes.getMeshCutOut(GCR.RL("gcr", texturePath)));
            });
        }

        return model;
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