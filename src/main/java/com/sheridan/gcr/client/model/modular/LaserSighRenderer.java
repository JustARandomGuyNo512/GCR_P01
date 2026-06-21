package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.fx.LaserEffectRenderer;
import com.sheridan.gcr.compat.IrisCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

@OnlyIn(Dist.CLIENT)
public class LaserSighRenderer {
    private final int color;
    private final ILaserSightModel laserSightModel;

    // 复用 FloatBuffer 避免每帧频繁分配内存
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    public LaserSighRenderer(ILaserSightModel laserSightModel, int color) {
        this.laserSightModel = laserSightModel;
        this.color = color;
    }

    public void renderFirstPerson(ModuleRenderContext context) {
        Bone laserPoseBone = laserSightModel.getLaserPoseBone();
        LaserEffectRenderer.recordEffectCall(
                color,
                context.currentRenderNode().id,
                laserPoseBone.renderStatus.pose,
                context);
        renderRay(true, laserPoseBone, context.partialTicks);
    }

    public void renderGeneric(ModuleRenderContext context) {
        renderRay(false, laserSightModel.getLaserPoseBone(), context.partialTicks);
    }

    public void renderRay(boolean firstPerson, Bone laserPoseBone, float partialTicks) {
        if (!IrisCompat.isRenderingShadowPass()) {
            boolean isIrisShaderInUse = Client.isIrisShaderInUse;


            final float length = firstPerson ? 64f : 5f;
            drawRay(laserPoseBone, length, partialTicks);
//            if (isIrisShaderInUse) {
//
//                Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
//                Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
//
//                Stage.HIGH.addTask(new Task(event -> {
//                    RenderSystem.getModelViewStack().pushMatrix();
//                    RenderSystem.getProjectionMatrix().set(projectionMatrix);
//                    RenderSystem.backupProjectionMatrix();
//                    RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
//
//
//                    RenderSystem.getModelViewMatrix().set(modelViewMatrix);
//
//
//                    drawRay(laserPoseBone, length, partialTicks);
//
//                    RenderSystem.getModelViewStack().popMatrix();
//                    RenderSystem.restoreProjectionMatrix();
//                }));
//            } else {
//
//                drawRay(laserPoseBone, length, partialTicks);
//            }
        }
    }

    public void drawRay(Bone laserPoseBone, float length, float partialTicks) {

    }
}