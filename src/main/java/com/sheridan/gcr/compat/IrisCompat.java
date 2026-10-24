package com.sheridan.gcr.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Oculus / Iris bridge for Forge 1.20.1.
 *
 * <p>Everything is resolved reflectively so GCR has neither a compile-time nor a runtime dependency on Oculus:
 * when the mod is absent every query falls back to the ordinary (vanilla shader) rendering path.</p>
 */
public final class IrisCompat {
    private static final String[] MOD_IDS = {"oculus", "iris"};

    private static boolean resolved = false;
    private static boolean present = false;
    private static Object apiInstance;
    private static Method isShaderPackInUse;
    private static Method isRenderingShadowPass;
    private static Field shadowModelView;
    private static Field shadowProjection;

    private IrisCompat() { }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            ModList list = ModList.get();
            boolean modLoaded = false;
            if (list != null) {
                for (String id : MOD_IDS) {
                    modLoaded |= list.isLoaded(id);
                }
            }
            if (!modLoaded) {
                return;
            }
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            apiInstance = api.getMethod("getInstance").invoke(null);
            isShaderPackInUse = api.getMethod("isShaderPackInUse");
            isRenderingShadowPass = api.getMethod("isRenderingShadowPass");
            try {
                Class<?> shadow = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
                shadowModelView = shadow.getField("MODELVIEW");
                shadowProjection = shadow.getField("PROJECTION");
            } catch (Throwable ignored) {
                // shadow matrices are optional
            }
            present = true;
        } catch (Throwable t) {
            present = false;
        }
    }

    private static boolean call(Method method) {
        resolve();
        if (!present || method == null) {
            return false;
        }
        try {
            return (Boolean) method.invoke(apiInstance);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean irisNotFound() {
        resolve();
        return !present;
    }

    public static boolean isShaderPackInUse() {
        return call(isShaderPackInUse);
    }

    public static boolean isRenderingShadowPass() {
        return call(isRenderingShadowPass);
    }

    private static Matrix4f readMatrix(Field field, Matrix4f fallback) {
        resolve();
        if (!present || field == null) {
            return fallback;
        }
        try {
            Object value = field.get(null);
            return value instanceof Matrix4f m ? m : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static Matrix4f getShadowProjectionMat() {
        return readMatrix(shadowProjection, RenderSystem.getProjectionMatrix());
    }

    public static Matrix4f getShadowModelViewMat() {
        return readMatrix(shadowModelView, RenderSystem.getModelViewMatrix());
    }
}
