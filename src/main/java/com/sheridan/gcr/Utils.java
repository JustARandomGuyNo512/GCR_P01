package com.sheridan.gcr;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.mixin.LightTextureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.lang.Math;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.UUID;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30C.*;

public class Utils {
    /**
     * 仅在客户端环境中使用此方法
     * 此方法的目的是复制给定的PoseStack对象
     * 复制操作包括创建一个新的PoseStack，并将给定的PoseStack的当前状态（包括变换矩阵和法线矩阵）复制到新对象中
     *
     * @param stack 需要复制的PoseStack对象
     * @return 复制后的PoseStack对象，具有与原对象相同的当前状态
     */
    @OnlyIn(Dist.CLIENT)
    public static PoseStack copyPoseStack(PoseStack stack) {
        PoseStack result = new PoseStack();
        result.setIdentity();
        result.last().pose().set(stack.last().pose());
        result.last().normal().set(stack.last().normal());
        return result;
    }

    /**
     * 仅在客户端环境中使用此方法
     * 检查是否启用了Iris光影
     *
     * @return 如果Iris着色器被启用并正在使用自定义着色器包，则返回true；否则返回false
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean isIrisShaderEnabled() {
        try {
            Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
            Field currentPackField = irisClass.getDeclaredField("currentPack");
            currentPackField.setAccessible(true);
            Object currentPack = currentPackField.get(null);
            return currentPack != null;
        } catch (Exception e) {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isStencilEnabled() {
        int result = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_STENCIL_ATTACHMENT,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        return result != GL11.GL_NONE;
    }


    @OnlyIn(Dist.CLIENT)
    public static void setUpStencil() {
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            return;
        }
        if (isStencilEnabled()) {
            return;
        }
        int depthTextureId = glGetFramebufferAttachmentParameteri(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        GL30.glBindTexture(GL_TEXTURE_2D, depthTextureId);
        GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_DEPTH24_STENCIL8,
                GL11C.glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH),
                GL11C.glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_HEIGHT),
                0, 34041, 34042, null);
        GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);
    }


    @OnlyIn(Dist.CLIENT)
    public static void fullGLContextPrint(int programId, int shaderId) {
        System.out.println("context test start: programId:" + programId + " shaderId:" + shaderId);
        IntBuffer countBuffer = BufferUtils.createIntBuffer(1);
        GL20.glGetProgramiv(programId, GL20.GL_ACTIVE_UNIFORMS, countBuffer);
        int uniformCount = countBuffer.get(0);
        System.out.println("debug start");
        for (int i = 0; i < uniformCount; i++) {
            IntBuffer sizeBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer typeBuffer = BufferUtils.createIntBuffer(1);
            String name = GL20.glGetActiveUniform(programId, i, 16, sizeBuffer, typeBuffer);
            int location = GL20.glGetUniformLocation(programId, name);

            System.out.println("Uniform #" + i + ": " + name +
                    " | Type: " + typeBuffer.get(0) +
                    " | Location: " + location);
        }
        printAllAttributes(shaderId);
        System.out.println("context test end: programId:" + programId + " shaderId:" + shaderId + "\n\n\n");
    }

    @OnlyIn(Dist.CLIENT)
    public static void printAllAttributes(int shaderProgram) {
        // 获取活动attribute数量
        int numAttributes = GL20C.glGetProgrami(shaderProgram, GL20C.GL_ACTIVE_ATTRIBUTES);

        System.out.println("Active Attributes: " + numAttributes);

        // 准备缓冲区
        IntBuffer sizeBuf = BufferUtils.createIntBuffer(1);
        IntBuffer typeBuf = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < numAttributes; i++) {
            // 获取attribute信息
            String attrName = GL20C.glGetActiveAttrib(shaderProgram, i, sizeBuf, typeBuf);
            int attrSize = sizeBuf.get(0);
            int attrType = typeBuf.get(0);


            // 获取attribute的位置(slot)
            int location = GL20C.glGetAttribLocation(shaderProgram, attrName);

            System.out.printf("Attribute #%d: %s (location=%d, size=%d, type=%s)%n",
                    i, attrName, location, attrSize, getTypeName(attrType));
        }
    }

    private static String getTypeName(int type) {
        return switch (type) {
            case GL20C.GL_FLOAT -> "float";
            case GL20C.GL_INT_VEC2 -> "vec2i";
            case GL20C.GL_FLOAT_VEC2 -> "vec2";
            case GL20C.GL_FLOAT_VEC3 -> "vec3";
            case GL20C.GL_FLOAT_VEC4 -> "vec4";
            case GL20C.GL_FLOAT_MAT2 -> "mat2";
            case GL20C.GL_FLOAT_MAT3 -> "mat3";
            case GL20C.GL_FLOAT_MAT4 -> "mat4";
            default -> "Unknown(" + type + ")";
        };
    }

    /**
     * 返回一个在给定的投影矩阵中，绝对位于视锥体外的位置，可用于ndc裁剪隐藏的物体或者跳过光栅化和fragment shader对某些物体的处理
     * @param projectionMatrix 投影矩阵，用于将坐标从视图空间转换到剪裁空间
     * @return 返回一个在视锥体外部的位置，以Vector3f形式表示
     */
    public static Vector3f getOutsideFrustumPosition(Matrix4f projectionMatrix) {
        Vector4f clipSpace = new Vector4f(2.0f, 0.0f, 0.0f, 1.0f);
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Vector4f viewSpace = invProj.transform(new Vector4f(clipSpace));
        viewSpace.div(viewSpace.w);
        return new Vector3f(viewSpace.x, viewSpace.y, viewSpace.z);
    }

    static Vector3f NONE = new Vector3f(1, 1, 1);

    @OnlyIn(Dist.CLIENT)
    public static Vector3f vanillaLightColorUnpack(int packedLight) {
        int u = packedLight & '\uffff';
        int v = packedLight >> 16 & '\uffff';
        return vanillaLightColorUnpack(u, v);
    }

    @OnlyIn(Dist.CLIENT)
    public static Vector3f vanillaLightColorUnpack(int u, int v) {
        LightTexture lightmap = Minecraft.getInstance().gameRenderer.lightTexture();
        NativeImage pixels = ((LightTextureAccessor) lightmap).getLightTexture().getPixels();
        if (pixels == null) {
            return NONE;
        }

        int color = pixels.getPixelRGBA(u / 16, v / 16);
        System.out.println("color:" + color);
        float b = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float r = (color & 0xFF) / 255f;
        return new Vector3f(r, g, b);
    }


    @OnlyIn(Dist.CLIENT)
    public static int secondToTick(float seconds) {
        return (int) (seconds * 20);
    }

    @OnlyIn(Dist.CLIENT)
    public static void overridePose(PoseStack stack, PoseStack.Pose pose) {
        stack.last().pose().set(pose.pose());
        stack.last().normal().set(pose.normal());
    }

    @OnlyIn(Dist.CLIENT)
    static final PoseStack TEMP = new PoseStack();
    @OnlyIn(Dist.CLIENT)
    public static PoseStack.Pose lerpPose(PoseStack.Pose from, PoseStack.Pose to, float progress) {
        progress = Mth.clamp(progress, 0, 1f);
        TEMP.setIdentity();
        lerpPoseStack(from, to, TEMP, progress, true, true, true);
        return TEMP.last().copy();
    }

    static final Vector3f V2_1 = new Vector3f();
    static final Vector3f V2_2 = new Vector3f();
    static final Vector3f V2_3 = new Vector3f();
    static final Vector3f V2_4 = new Vector3f();
    @OnlyIn(Dist.CLIENT)
    public static void lerpPoseStack(PoseStack.Pose from, PoseStack.Pose to, PoseStack res, float progress, boolean translation, boolean rotation, boolean scale) {
        if (translation || rotation || scale) {
            Matrix4f fromPose = from.pose();
            Matrix4f toPose = to.pose();
            if (translation) {
                Vector3f fromTranslation = fromPose.getTranslation(V2_1);
                Vector3f toTranslation = toPose.getTranslation(V2_2);
                res.translate(
                        fromTranslation.x + (toTranslation.x - fromTranslation.x) * progress,
                        fromTranslation.y + (toTranslation.y - fromTranslation.y) * progress,
                        fromTranslation.z + (toTranslation.z - fromTranslation.z) * progress);
            }
            if (rotation) {
                Quaternionf normalizedRotation1 = from.normal().getNormalizedRotation(new Quaternionf());
                Quaternionf normalizedRotation2 = to.normal().getNormalizedRotation(new Quaternionf());
                res.mulPose(normalizedRotation1.nlerp(normalizedRotation2, progress));
            }
            if (scale) {
                Vector3f fromScale = fromPose.getScale(V2_3);
                Vector3f toScale = toPose.getScale(V2_4);
                float sx = fromScale.x + (toScale.x - fromScale.x) * progress;
                float sy = fromScale.y + (toScale.y - fromScale.y) * progress;
                float sz = fromScale.z + (toScale.z - fromScale.z) * progress;
                res.scale(sx, sy, sz);
            }
        }
    }

    static final Vector3f V3_1 = new Vector3f();
    static final Vector3f V3_2 = new Vector3f();
    static final Vector3f V3_3 = new Vector3f();
    static final Vector3f V3_4 = new Vector3f();
    static final Matrix3f rotationMatrix = new Matrix3f();
    public static Quaternionf extractPureRotation(Matrix4f matrix) {
        Vector3f scale = matrix.getScale(V3_1);
        matrix.get3x3(rotationMatrix);
        rotationMatrix.getRow(0, V3_2).div(scale.x);
        rotationMatrix.getRow(1, V3_3).div(scale.y);
        rotationMatrix.getRow(2, V3_4).div(scale.z);
        rotationMatrix.setRow(0, V3_2);
        rotationMatrix.setRow(1, V3_3);
        rotationMatrix.setRow(2, V3_4);
        Quaternionf rotation = new Quaternionf();
        rotation.setFromNormalized(rotationMatrix);
        return rotation;
    }

    public static long uuidToSeed(UUID uuid) {
        long x = uuid.getMostSignificantBits();
        long y = uuid.getLeastSignificantBits();

        long seed = x ^ Long.rotateLeft(y, 32);

        seed ^= (seed >>> 33);
        seed *= 0xff51afd7ed558ccdL;
        seed ^= (seed >>> 33);
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= (seed >>> 33);

        return seed;
    }

    @OnlyIn(Dist.CLIENT)
    public static float getDepthByProjectionMat(Vector3f translation, Matrix4f projectionMatrix)  {
        Vector4f vector4f = new Vector4f(translation.x, translation.y, translation.z, 1.0f);
        Vector4f coord = vector4f.mul(RenderSystem.getModelViewMatrix()).mul(projectionMatrix);
        return (coord.z / coord.w) / 2f + 0.5f;
    }

    /**
     * 模拟阻尼振荡函数
     * @param t 时间（秒）
     * @param A 初始振幅
     * @param omega0 固有角频率（rad/s）
     * @param zeta 阻尼比（0 ~ 1）
     * @param phi 初始相位（rad）
     * @return 振幅值 x(t)
     */
    public static double dampedOscillation(float t, float A, float omega0, float zeta, float phi) {
        return A * Math.exp(-zeta * omega0 * t) * Math.cos(omega0 * t + phi);
    }


    public static Vector3f getScreenPos(Matrix4f localPose, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float width, float height) {
        Vector4f vector4f = localPose.transform(new Vector4f(0, 0, 0, 1.0F));
        Vector4f v = vector4f.mul(modelViewMatrix).mul(projectionMatrix);

        float screenX = ((v.x / v.w) * width + width) * 0.5f;
        float screenY = (-(v.y / v.w) * height + height) * 0.5f;
        float screenDepth = v.z;
        return new Vector3f(screenX, screenY, screenDepth);
    }

    public static float sCurve(float val) {
        return 3f * val * val - 2f * val * val * val;
    }
}
