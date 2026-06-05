package com.sheridan.gcr.client.render;


import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class AuxRenderTarget extends TextureTarget {
    private static AuxRenderTarget INSTANCE;

    public static AuxRenderTarget getInstance() {
        if (INSTANCE == null) {
            RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
            INSTANCE = new AuxRenderTarget(mainRenderTarget.width, mainRenderTarget.height, true);
        }
        return INSTANCE;
    }

    public AuxRenderTarget(int width, int height, boolean clearError) {
        super(width, height, true, clearError);
    }

    public void check() {
        RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.isStencilEnabled()) {
            enableStencil();
        }
        int mainWidth = mainRenderTarget.width;
        int mainHeight = mainRenderTarget.height;
        if (mainWidth != width || mainHeight != height) {
            resize(mainWidth, mainHeight, true);
        }
    }

    public void clearDepth() {
        GL11.glDepthMask(true);
        GL11.glClearDepth(1.0D);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void clearStencil() {
        GL11.glStencilMask(0xFF);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0x00);
    }

    public void clearAndPrepare() {
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glStencilMask(0xFF);
        GL11.glClearStencil(0);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0x00);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
}