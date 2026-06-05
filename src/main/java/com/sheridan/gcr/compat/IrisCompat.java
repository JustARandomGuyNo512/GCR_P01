package com.sheridan.gcr.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

import java.lang.reflect.Field;

public class IrisCompat {
    private static Class<?> irisApiClass;
    private static Object irisApiInstance;
    private static java.lang.reflect.Method isShaderPackInUseMethod;
    private static java.lang.reflect.Method isRenderingShadowPassMethod;
    private static Field projectionField;
    private static Field modelviewField;

    static {
        Class<?> shadowRendererClass;
        try {
            irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

            java.lang.reflect.Method getInstanceMethod = irisApiClass.getMethod("getInstance");
            irisApiInstance = getInstanceMethod.invoke(null);

            isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            isRenderingShadowPassMethod = irisApiClass.getMethod("isRenderingShadowPass");

            shadowRendererClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
            projectionField = shadowRendererClass.getField("PROJECTION");

            shadowRendererClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
            modelviewField = shadowRendererClass.getField("MODELVIEW");
        } catch (Throwable t) {
            irisApiClass = null;
            irisApiInstance = null;
            isShaderPackInUseMethod = null;
            isRenderingShadowPassMethod = null;
            projectionField = null;
            modelviewField = null;
        }
    }

    public static boolean irisNotFound() {
        return irisApiClass == null || irisApiInstance == null;
    }

    public static boolean isShaderPackInUse() {
        if (irisNotFound()) {
            return false;
        }
        try {
            return (boolean) isShaderPackInUseMethod.invoke(irisApiInstance);
        } catch (Exception e) {
            return false;
        }
    }

    public static Matrix4f getShadowProjectionMat() {
        if (projectionField == null) {
            return RenderSystem.getProjectionMatrix();
        }
        try {
            Matrix4f mat = (Matrix4f) projectionField.get(null);
            return mat == null ? RenderSystem.getProjectionMatrix() : mat;
        } catch (Exception e) {
            return RenderSystem.getProjectionMatrix();
        }
    }

    public static Matrix4f getShadowModelViewMat() {
        if (modelviewField == null) {
            return RenderSystem.getModelViewMatrix();
        }
        try {
            Matrix4f mat = (Matrix4f) modelviewField.get(null);
            return mat == null ? RenderSystem.getModelViewMatrix() : mat;
        } catch (Exception e) {
            return RenderSystem.getModelViewMatrix();
        }
    }

    public static boolean isRenderingShadowPass() {
        if (irisNotFound()) {
            return false;
        }
        try {
            return (boolean) isRenderingShadowPassMethod.invoke(irisApiInstance);
        } catch (Exception e) {
            return false;
        }
    }
}
