package com.sheridan.gcr.client.aiStuff;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.ModelRegistrationManager;
import com.sheridan.gcr.client.model.BufferedBoneMeshModel;
import com.sheridan.gcr.client.model.gltf.io.GltfModelLoader;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.client.render.HeatMapTextureManager;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 客户端模型热重载：按模块（{@code modular_id}）重置单个模型。
 *
 * <p>唯一的触发方式是命令 {@code /gcr:reload_model <modular_id>}（见 {@link ModelReloadCommand}），
 * 执行流程为：</p>
 * <ol>
 *     <li>通过 {@link ModuleRegister#get(String)} 找到模块实例，并从
 *     {@link ModelRegistrationManager} 取出它注册模型时记录下来的“配方”（gltf / 纹理 / 工厂 Lambda）。</li>
 *     <li><b>重新读取文件</b>：删掉 jME3 里这个 gltf 的缓存条目，再重新解析——开发环境下
 *     {@link DevAssetResolver} 会优先读 {@code src/main/resources}，所以不需要重新跑 processResources。</li>
 *     <li><b>卸载旧对象</b>：把旧模型从 {@link ModuleModelRegister} 摘掉并释放它的 VBO/UBO。</li>
 *     <li><b>重新装载</b>：把新模型登记回 {@link ModuleModelRegister}，并重新从磁盘读取它的网格纹理/热力图纹理。</li>
 *     <li><b>编译模型</b>：重新上传顶点数据，重建状态机映射，并让渲染侧缓存失效。</li>
 * </ol>
 *
 * <p>重建与卸载的先后顺序是刻意安排的：先把新模型造出来（这一步可能因为文件写坏而失败），
 * 成功之后再替换，失败时旧模型仍然完好可用，相当于一次原子替换。</p>
 *
 * <p>只能在客户端主线程（即渲染线程）执行；若从别的线程调用会自动切回主线程。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientModelHotReloader {

    private static boolean reloading;

    private ClientModelHotReloader() {
    }

    /**
     * 重置指定模块的客户端模型。
     *
     * @param modularId {@link ModuleRegister} 中的模块 id，例如 {@code gcr:m4a1}
     * @return 是否已经（同步地）完成重载；跨线程派发时返回 true 表示任务已提交
     */
    public static boolean reloadModel(String modularId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        if (!minecraft.isSameThread()) {
            // 客户端命令可能在网络线程或集成服务器线程上被派发，统一切回客户端主线程再执行
            minecraft.execute(() -> reloadModel(modularId));
            return true;
        }
        if (reloading) {
            GCR.LOGGER.warn("[GCR HotReload] reload already in progress, request ignored");
            return false;
        }

        // 1. 通过 ModuleRegister 找到模块实例
        if (modularId == null || modularId.isBlank()) {
            notifyPlayer("用法: /" + ModelReloadCommand.COMMAND + " <" + ModelReloadCommand.ARG_MODULAR_ID + ">");
            return false;
        }
        IModular module = ModuleRegister.get(modularId);
        if (module == null) {
            notifyPlayer("找不到模块: " + modularId);
            return false;
        }
        if (!ModelRegistrationManager.hasRecipe(module)) {
            notifyPlayer("模块 " + modularId + " 没有登记客户端模型");
            return false;
        }

        reloading = true;
        long startedAt = System.nanoTime();
        // 武器线程（500Hz 的 ClientWeaponLooper）会并发读取注册表，替换模型期间必须与它互斥
        Client.LOCK.lock();
        try {
            resetModel(module);

            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
            GCR.LOGGER.info("[GCR HotReload] reloaded client model of {} in {} ms", modularId, elapsedMs);
            notifyPlayer("已热重载模型: " + modularId + "，用时 " + elapsedMs + " ms");
            return true;
        } catch (Throwable t) {
            GCR.LOGGER.error("[GCR HotReload] failed to reload client model of {}", modularId, t);
            notifyPlayer("热重载 " + modularId + " 失败: " + t);
            return false;
        } finally {
            Client.LOCK.unlock();
            reloading = false;
        }
    }

    /**
     * 单个模块的“重新读取 → 重新装载 → 编译”流程。
     */
    private static void resetModel(IModular module) {
        ResourceLocation modelPath = ModelRegistrationManager.getModelPath(module);
        ResourceLocation texturePath = ModelRegistrationManager.getModelTexture(module);

        // 1. 重新读取文件：先删掉 jME3 对这个 gltf 的缓存，否则 loadModel 会直接返回上一次解析出来的旧模型
        GltfModelLoader.clearModelCache(modelPath);

        // 2. 重新装载：重跑注册时那段工厂 Lambda，得到全新的模型实例
        //    （这一步失败会抛异常，此时旧模型还在注册表里，游戏不会因为一个坏文件而失去这个模型）
        IModularModel model = ModelRegistrationManager.createModel(module);
        if (model == null) {
            throw new IllegalStateException("module has no model recipe: " + module.getID());
        }

        // 3. 卸载旧对象：从 ModuleModelRegister 摘掉并释放它占用的 GPU 资源
        IModularModel old = ModuleModelRegister.remove(module);
        ResourceLocation oldHeatMapPath = old == null ? null : old.getHeatMapTexPath();
        if (old instanceof BufferedBoneMeshModel buffered) {
            buffered.release();
        }

        // 4. 注册新模型
        ModuleModelRegister.register(module, model);

        // 5. 纹理重新从磁盘读取：网格纹理由 DevSimpleTexture 强制重读，热力图需要额外同步 HeatMapTextureManager 的缓存
        if (texturePath != null) {
            DevSimpleTexture.registerFromDisk(texturePath);
        }
        HeatMapTextureManager.reloadCachedTexture(oldHeatMapPath);
        HeatMapTextureManager.reloadCachedTexture(model.getHeatMapTexPath());

        // 6. 编译模型：把顶点数据重新上传到 GPU
        if (texturePath != null) {
            model.compile(RenderTypes.getMeshCutOut(texturePath));
        }

        // 7. 重建这个模型的状态映射（等价于启动流程里的 afterModelRegister，但只处理当前模型）
        if (model instanceof IStateViewerModel<?> stateModel) {
            IStateViewer<?> viewer = stateModel.getViewer();
            if (viewer != null) {
                viewer.onRegisterStateMapping();
            }
        }

        // 8. 渲染缓存里还留着旧模型实例（已释放），必须失效；下一帧会按新的注册表重建渲染树
        Client.getGunRenderer().invalidateRenderCache();
        Client.WEAPON_STATUS.invalidateModelCache();
    }

    /** 只会在客户端主线程上被调用，因此这里可以直接把消息发给玩家。 */
    private static void notifyPlayer(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(message));
        }
    }
}
