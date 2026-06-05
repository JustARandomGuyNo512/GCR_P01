package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.platform.GlStateManager;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.RenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class RedDotModel extends AbstractScopeModel {

    public RedDotModel(MeshModelData root, ResourceLocation name, ResourceLocation crosshairTexture, float crosshairScale) {
        super(root, name, crosshairTexture, crosshairScale);
    }

    @Override
    public void render(ModuleRenderContext context) {
        if (shouldHandlerScopeRender(context)) {
            Bone rearLensBone = getRearLensBone();
            rearLensBone.renderStatus.visible = false;
            super.render(context);
            drawRearLensStencilMask(context);
        } else {
            super.render(context);
        }
    }

    protected void drawRearLensStencilMask(ModuleRenderContext context) {
        renderingVertexCount = 1;
        Bone rearLensBone = getRearLensBone();
        rearLensBone.renderStatus.visible = true;
        renderRearLensOnly = true;

        RenderType tempRenderType = getRenderType();
        setRenderType(RenderTypes.getMeshStencilMask(), false);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GlStateManager._stencilMask(0xFF);
        GlStateManager._stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        super.render(true);
        renderRearLensOnly = false;

        setRenderType(tempRenderType, false);
        GlStateManager._stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GlStateManager._stencilMask(0x00);

        Bone crosshairBone = getCrosshairBone();
        Bone crosshairZBone = getCrosshairZBone();
        renderCrosshairTexture(crosshairBone.renderStatus.pose, crosshairZBone.renderStatus.pose);

        clearAndDisableStencil();

        renderRearLensOnly = false;
    }
}
