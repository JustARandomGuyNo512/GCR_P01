package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.fx.LaserModel;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.client.render.fx.LaserEffectRenderer;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.renderer.RenderType;
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
        renderRay(true, laserPoseBone, context);
    }

    public void renderGeneric(ModuleRenderContext context) {
        renderRay(false, laserSightModel.getLaserPoseBone(), context);
    }

    public void renderRay(boolean firstPerson, Bone laserPoseBone, ModuleRenderContext context) {
        if (!IrisCompat.isRenderingShadowPass()) {
            PoseStack poseStack = new PoseStack();
            poseStack.last().pose().set(laserPoseBone.renderStatus.pose.pose());
            VertexConsumer vertexConsumer = context.getBuffer(RenderType.energySwirl(LaserModel.LASER_TEXTURE, 0, 0));
            if (firstPerson) {
                float hitLength = LaserEffectRenderer.getHitLength(context.currentRenderNode().id);
                if (Float.isNaN(hitLength)) {
                    return;
                }
                float length = (float) ((hitLength + 1) / 0.625f + Math.random() * (hitLength * 0.3f + 5f));
                LaserModel.INSTANCE.renderFirstPerson(poseStack, vertexConsumer, color, length);
            } else {
                LaserModel.INSTANCE.renderThirdPerson(poseStack, vertexConsumer, color);
            }
        }
    }

}