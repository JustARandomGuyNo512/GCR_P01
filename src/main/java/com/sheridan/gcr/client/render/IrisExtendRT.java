package com.sheridan.gcr.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL44;

import java.nio.ByteBuffer;
import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public class IrisExtendRT {
    public static int textureId = -1;
    public static int attachmentLayoutLocation = -1;
    public static int lastUsingFbo = -1;
    public static boolean dirty = false;
    public static int[] attachments = null;

    public static int lastWidth = 0;
    public static int lastHeight = 0;

    public static void updateAttachmentLayout(int location) {
        System.out.println("update layout location to: " + location);
        if (location == -1) {
            attachments = null;
        } else {
            attachments = new int[location + 1];
            for (int i = 0; i <= location; i++) {
                attachments[i] = GL30.GL_COLOR_ATTACHMENT0 + i;
            }
        }
        System.out.println("attachment layout locations: " + Arrays.toString(attachments));
        attachmentLayoutLocation = location;
    }

    public static void setUpDrawBuffers() {
        if (attachments == null || textureId == -1 || lastUsingFbo == -1) {
            return;
        }
        GL32C.glDrawBuffers(attachments);
        dirty = true;
    }

    public static void clearMuzzleTexture() {
        if (dirty && lastUsingFbo != -1 && textureId != -1 && attachmentLayoutLocation != -1) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lastUsingFbo);
            GL30.glClearBufferfv(GL30.GL_COLOR, attachmentLayoutLocation, new float[]{0, 0, 0, 0});
            dirty = false;
        }
    }

    /*
    * 必须在iris resize fbo之前进行分辨率检测，然后提前detach，否则N卡平台会：
    * GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS
    * 然后直接爆炸！
    * */
    public static void preFrameCheck(int expectedWidth, int expectedHeight) {
        if (attachmentLayoutLocation == -1) {
            return;
        }

        boolean sizeChanged = (expectedWidth != lastWidth || expectedHeight != lastHeight);

        if (!sizeChanged) {
            return;
        }
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            // 如果 FBO 不完整，直接 return，不进行后续的销毁和重建
            return;
        }
        System.out.println("[MuzzleRT] Resize detected: " + lastWidth + "x" + lastHeight + " -> " + expectedWidth + "x" + expectedHeight);

        // 如果之前 attach 过，先 detach
        if (lastUsingFbo != -1) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lastUsingFbo);

            int attachment = GL30.GL_COLOR_ATTACHMENT0 + attachmentLayoutLocation;

            GL30.glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER,
                    attachment,
                    GL11.GL_TEXTURE_2D,
                    0,
                    0
            );

            lastUsingFbo = -1;
        }

        // 删除旧 texture
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }

        //创建新 texture（用“目标尺寸”，不是从 FBO 读）
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                expectedWidth,
                expectedHeight,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GL44.glClearTexImage(textureId, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        lastWidth = expectedWidth;
        lastHeight = expectedHeight;

        System.out.println("[MuzzleRT] Texture rebuilt: " + textureId);
    }

    public static void ensureMuzzleAttachment() {
        if (attachmentLayoutLocation == -1) {
            return;
        }

        if (textureId == -1) {
            return;
        }

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            lastUsingFbo = -1;
            return;
        }

        int fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int attachment = GL30.GL_COLOR_ATTACHMENT0 + attachmentLayoutLocation;

        int type = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER,
                attachment,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );

        // 已经 attach 就不用管
        if (type != GL11.GL_NONE) {
            lastUsingFbo = fbo;
            return;
        }

        // attach
        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                attachment,
                GL11.GL_TEXTURE_2D,
                textureId,
                0
        );

        lastUsingFbo = fbo;

        System.out.println("[MuzzleRT] Attached to FBO " + fbo);
    }


}
