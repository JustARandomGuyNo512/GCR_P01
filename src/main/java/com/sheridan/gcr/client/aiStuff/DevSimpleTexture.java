package com.sheridan.gcr.client.aiStuff;

import com.mojang.blaze3d.platform.NativeImage;
import com.sheridan.gcr.GCR;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * 一个会优先从开发源目录读取 PNG 的 {@link SimpleTexture}。
 *
 * <p>网格纹理由 {@code TextureStateShard} 在绘制时通过 {@code TextureManager.getTexture(RL)}
 * 惰性创建，创建出来的对象是原版 {@link SimpleTexture}，走的一直是原版 {@link ResourceManager}
 * （也就是 {@code build/resources/main} 里的构建副本）。模型热重载时用
 * {@link #registerFromDisk(ResourceLocation)} 把纹理对象替换成本类，
 * 就能强制重新从磁盘读取最新的 PNG。</p>
 *
 * <p>非开发环境（{@link DevAssetResolver#enabled()} 为 false）下行为与 {@link SimpleTexture} 完全一致，
 * 只是从 mod 包内重新读取一次。</p>
 */
@OnlyIn(Dist.CLIENT)
public class DevSimpleTexture extends SimpleTexture {

    public DevSimpleTexture(ResourceLocation location) {
        super(location);
    }

    /**
     * 重新注册一张纹理，强制立即从磁盘读取最新内容（开发环境优先源目录）。
     *
     * <p>{@code TextureManager.register} 会同步调用 {@code load}，同时关闭并释放被替换掉的旧纹理。
     * 返回的是管理器最终登记的对象（加载失败时会退化为缺失纹理）。</p>
     *
     * @return 注册后 {@code TextureManager} 中实际保存的纹理对象
     */
    public static AbstractTexture registerFromDisk(ResourceLocation location) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        textureManager.register(location, new DevSimpleTexture(location));
        return textureManager.getTexture(location, MissingTextureAtlasSprite.getTexture());
    }

    @Override
    protected TextureImage getTextureImage(ResourceManager resourceManager) {
        if (DevAssetResolver.hasSourceFile(this.location)) {
            try (InputStream in = DevAssetResolver.openOrNull(this.location, resourceManager)) {
                if (in != null) {
                    NativeImage image = NativeImage.read(in);
                    // 不读取 .mcmeta：与现有的 SimpleTexture 使用方式(blur=false, clamp=false)保持一致
                    return new TextureImage(null, image);
                }
            } catch (IOException e) {
                GCR.LOGGER.warn("[GCR HotReload] failed to read dev texture {}, falling back to pack copy: {}",
                        this.location, e.toString());
            } catch (UncheckedIOException ignored) {
                // openOrNull 已记录日志
            }
        }
        return super.getTextureImage(resourceManager);
    }
}
