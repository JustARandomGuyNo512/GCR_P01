package com.sheridan.gcr.client.model.gltf.io;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.sheridan.gcr.client.aiStuff.DevAssetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class GltfAssetsLocator implements AssetLocator {

    /**
     * 设置资源定位的根路径
     */
    @Override
    public void setRootPath(String rootPath) {}

    /**
     * 定位并返回指定资源键对应的资产信息对象。
     *
     * <p>资源流的优先级为：开发源目录（{@code src/main/resources}）→ 原版 {@code ResourceManager}。
     * 前者让热重载能读到刚刚保存到工程里的 gltf，后者是正常运行时的唯一来源。</p>
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
            // 先确认资源确实存在，避免把一个必定失败的 AssetInfo 交给 jME3
            if (!DevAssetResolver.exists(path, Minecraft.getInstance().getResourceManager())) {
                return null;
            }
            return new AssetInfo(manager, key) {
                /**
                 * 每次都重新打开一条流，保证解析过程中拿到的都是磁盘上的最新内容。
                 * @return 资源的输入流
                 */
                @Override
                public InputStream openStream() {
                    InputStream stream = DevAssetResolver.openOrNull(path, Minecraft.getInstance().getResourceManager());
                    if (stream == null) {
                        throw new UncheckedIOException(new IOException("Resource not found: " + path));
                    }
                    return stream;
                }
            };

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
