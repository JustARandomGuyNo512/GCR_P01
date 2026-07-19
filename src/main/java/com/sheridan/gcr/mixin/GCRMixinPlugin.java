package com.sheridan.gcr.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Set;

public class GCRMixinPlugin implements IMixinConfigPlugin {

    private static final String IRIS_MIXIN_TEXTURES = "com.sheridan.gcr.mixin.MixinGlStateManagerTextures";
    private static Boolean shaderModPresent = null;
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals(IRIS_MIXIN_TEXTURES)) {
            return !isShaderModPresent();
        }
        return true;
    }

    private boolean isShaderModPresent() {
        if (shaderModPresent == null) {
            shaderModPresent = classExists("net.irisshaders.iris.Iris")
                    || classExists("net.coderbot.iris.Iris")
                    || classExists("net.irisshaders.iris.api.v0.IrisApi");
            System.out.println("[GCR] shader mod detected: " + shaderModPresent);
        }
        return shaderModPresent;
    }

    private boolean classExists(String className) {
        try {
            // initialize = false：只做类查找/解析，不触发 <clinit>，避免副作用和循环加载问题
            Class.forName(className, false, this.getClass().getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}