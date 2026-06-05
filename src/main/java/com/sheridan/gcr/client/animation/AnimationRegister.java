package com.sheridan.gcr.client.animation;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class AnimationRegister {

    private static final Map<String, Map<String, AnimationDef>> ANIMATIONS = new HashMap<>();

    @Nullable
    public static AnimationDef get(ResourceLocation path) {
        Map<String, AnimationDef> stringAnimationDefMap = ANIMATIONS.get(path.getNamespace());
        if (stringAnimationDefMap != null) {
            return stringAnimationDefMap.get(path.getPath());
        }
        return null;
    }

    public static boolean register(ResourceLocation path, AnimationDef animation) {
        String namespace = path.getNamespace();
        if (!ANIMATIONS.containsKey(namespace)) {
            ANIMATIONS.put(namespace, new HashMap<>());
        }
        Map<String, AnimationDef> container = ANIMATIONS.get(namespace);
        if (container.containsKey(path.getPath())) {
            return false;
        } else {
            container.put(path.getPath(), animation);
        }
        return true;
    }
}
