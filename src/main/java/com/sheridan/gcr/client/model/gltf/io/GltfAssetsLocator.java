package com.sheridan.gcr.client.model.gltf.io;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

public class GltfAssetsLocator implements AssetLocator {

    /**
     * 设置资源定位的根路径
     */
    @Override
    public void setRootPath(String rootPath) {}

    /**
     * 定位并返回指定资源键对应的资产信息对象。
     *
     * @param manager 资产管理器实例。
     * @param key     资产键对象，包含资源名称。
     * @return 返回一个AssetInfo实例，用于读取资源流；如果找不到资源或发生错误，则返回null。
     */
    @Override
    public AssetInfo locate(AssetManager manager, AssetKey key) {
        // 将资源名称解析为ResourceLocation对象
        ResourceLocation path = ResourceLocation.parse(key.getName());
        try {
            // 获取Minecraft实例的资源管理器
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            // 查找指定路径的资源
            Resource resource = resourceManager.getResource(path).orElse(null);
            // 如果未找到资源，输出错误信息并返回null
            if (resource == null) {
                return null;
            }
            // 打开资源输入流
            InputStream stream = resource.open();
            // 创建并返回一个新的AssetInfo匿名子类实例
            return new AssetInfo(manager, key) {
                /**
                 * 打开资源流以供后续处理。
                 * @return 返回资源的输入流。
                 */
                @Override
                public InputStream openStream() {
                    return stream;
                }
            };

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
