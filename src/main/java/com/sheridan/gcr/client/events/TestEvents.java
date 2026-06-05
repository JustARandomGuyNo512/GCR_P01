package com.sheridan.gcr.client.events;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;


@OnlyIn(Dist.CLIENT)
public class TestEvents {
    public static boolean k = false;
    public static RenderTarget renderTarget = null;

    public static RenderTarget tempMainTarget = null;

//    @SubscribeEvent
//    public static void test1(RenderHandEvent event) {
//        if (event.getHand() == InteractionHand.MAIN_HAND) {
//            k = true;
//        } else {
//            k = false;
//        }
//
//    }

//    static int tried = 0;
//
    @SubscribeEvent
    public static void test(ClientTickEvent.Pre event) {
    //        LocalPlayer player = Minecraft.getInstance().player;
    //        if (player == null) {
    //            return;
    //        }
    //        System.out.println(System.identityHashCode(player.getMainHandItem()));
    }
//
//    public static void setUp() {
//        try {
//            if (tried >= 0) {
//                return;
//            }
//            //LocalPlayer player = Minecraft.getInstance().player;
//            //if (player != null && player.getMainHandItem().getItem() == Items.APPLE) {
//
//            //}
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        //throw new RuntimeException("Stop !!!");
//    }
//
//
//
//    public static float fovModify = 70;
//
//    static boolean k = false;
//
//    static boolean c = false;
//
//    static boolean saved = false;
//
//    static int f = 0;
//
//    static PipScopeViewRenderer renderer = null;
//    @SubscribeEvent
//    public static void test4(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
//            return;
//        }
//        if (renderer != null && k) {
//            //_blitToScreen(renderer2.getRenderTarget().width, renderer2.getRenderTarget().height, false, renderer2.getRenderTarget());
//            k = false;
//            RenderTarget renderTarget = renderer.getRenderTarget();
//            if (!saved && f == 100) {
//                saveRenderTargetToFile(renderTarget, "test.png");
//                saved = true;
//            } else {
//                f ++;
//            }
//        }
//    }
//



//   // @SubscribeEvent
//        public static void test2(RenderHandEvent event) {
//
//        }
//
//    static int c = 0;
//    static boolean k = false;
//    //@SubscribeEvent
//    public static void test() {
////        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
////            return;
////        }
////        if (Client.getAimingProgress() < 0.9f) {
////            return;
////        }
//        if (Minecraft.getInstance().player == null ||
//                !(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof GunItem)) {
//            k = false;
//            c = 0;
//            return;
//        }
//        c ++;
//        if (c < 200) {
//            return;
//        }
//        if (k) {
//            return;
//        }
//        net.irisshaders.iris.Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> {
//            if (pipeline instanceof net.irisshaders.iris.pipeline.ShaderRenderingPipeline irisPipeline) {
//
//                IrisRenderTargetsAccessor accessor =
//                        (IrisRenderTargetsAccessor) irisPipeline;
//
//                net.irisshaders.iris.targets.RenderTargets targets =
//                        accessor.getRenderTargets();
//
//                IrisRenderTargetsFieldAccessor fieldAccessor =
//                        (IrisRenderTargetsFieldAccessor) targets;
//
//
//                int textureId = fieldAccessor.getNoTranslucents().getTextureId();
//
//                Window window = Minecraft.getInstance().getWindow();
//                saveDepthTexture(textureId, window.getWidth(), window.getHeight(), "depth1.png");
//            }
//        });
    ////        int depthTexture = IrisCompat.getIrisHandLayerDepthTexture();
    ////        Window window = Minecraft.getInstance().getWindow();
    ////        saveDepthTexture(depthTexture, window.getWidth(), window.getHeight(), "depth1.png");
    ////        k = true;
//    }


    public static void debugReadFloatAttachment(int width, int height, String path) {
        // 1. 分配浮点缓冲区 (width * height * 1个通道 * 4字节)
        java.nio.FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(width * height);

        // 2. 使用 GL_RED 和 GL_FLOAT 读取单通道浮点数据
        GL11.glReadPixels(
                0, 0,
                width, height,
                GL11.GL_RED,
                GL11.GL_FLOAT,
                floatBuffer
        );

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = floatBuffer.get(x + (height - 1 - y) * width);

                // 将浮点值映射到可见像素 (假设你的值在 0.0 - 1.0 之间)
                // 如果你的值是 100.0，建议 val / 100.0f 映射一下
                int c = (int) (Math.max(0, Math.min(1.0, val)) * 255);
                int rgb = (c << 16) | (c << 8) | c;

                image.setRGB(x, y, rgb);
            }
        }

        try {
            ImageIO.write(image, "PNG", new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void saveCurrentFramebufferColor(int width, int height, String path) {
        System.out.println("Saving current framebuffer color...");

        // === 读取当前 framebuffer ===
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        GL11.glReadPixels(
                0, 0,
                width, height,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                buffer
        );

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int i = (x + (height - 1 - y) * width) * 4;

                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;

                int argb = (a << 24) | (r << 16) | (g << 8) | b;

                image.setRGB(x, y, argb);
            }
        }

        try {
            ImageIO.write(image, "PNG", new File(path));
            System.out.println("Saved framebuffer to " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveDepthTexture(int texId, int width, int height, String path) {
        System.out.println("try save depth texture");
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(width * height);

        GL11.glGetTexImage(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT,
                buffer
        );

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int index = x + (height - 1 - y) * width;

                float depth = buffer.get(index);

                int gray = (int)(depth * 255.0f);
                gray = Math.max(0, Math.min(255, gray));

                int argb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;

                image.setRGB(x, y, argb);
            }
        }

        try {
            ImageIO.write(image, "PNG", new File(path));
            System.out.println("Saved depth to " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//
//    private static void _blitToScreen(int width, int height, boolean disableBlend, RenderTarget target) {
//        RenderSystem.assertOnRenderThread();
//        GlStateManager._colorMask(true, true, true, false);
//        GlStateManager._disableDepthTest();
//        GlStateManager._depthMask(false);
//        GlStateManager._viewport(0, 0, width, height);
//        if (disableBlend) {
//            GlStateManager._disableBlend();
//        }
//
//        Minecraft minecraft = Minecraft.getInstance();
//        ShaderInstance shaderinstance = (ShaderInstance) Objects.requireNonNull(minecraft.gameRenderer.blitShader, "Blit shader not loaded");
//        shaderinstance.setSampler("DiffuseSampler", target.getColorTextureId());
//        shaderinstance.apply();
//        BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
//        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
//        bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
//        bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
//        bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
//        BufferUploader.draw(bufferbuilder.buildOrThrow());
//        shaderinstance.clear();
//        GlStateManager._depthMask(true);
//        GlStateManager._enableDepthTest();
//        GlStateManager._colorMask(true, true, true, true);
//    }
}