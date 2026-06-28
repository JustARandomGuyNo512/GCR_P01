package com.sheridan.gcr.client.render.fx.muzzleFlash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.client.render.RenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;


@OnlyIn(Dist.CLIENT)
public class MuzzleFlashTexture {
    private static final float BASE_ALPHA = 0.9f;
    private final int count;
    private final float quadSize = 0.25f;
    private final RenderType renderType;

    public MuzzleFlashTexture(ResourceLocation texture, int count) {
        //quadSize = 1f / count;
        this.count = count;
        this.renderType = RenderTypes.getMuzzleFlash(texture);
    }

    public int getCount() {
        return count;
    }

    public void render(int index, PoseStack.Pose pose, MultiBufferSource buffer, boolean isFirstPerson) {
        if (index >= 0 && index < count) {
            VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
            if (!isFirstPerson) {
                draw(pose, 0, 0, 0, 0, vertexConsumer, index);
                draw(pose, 2, 1.5707963267948966f,  0,  -0.5f,  vertexConsumer, index);
                draw(pose, 1, 1.5707963267948966f, -1.5707963267948966f,   -0.5f,  vertexConsumer, index);
            }
            if (isFirstPerson) {
                draw(pose, 0, 0, 0, 0, vertexConsumer, index);
            }
        }
    }

    private void draw(PoseStack.Pose pose,int axis, float rx, float ry, float tz, VertexConsumer vertexConsumer, int index) {
        PoseStack.Pose renderPose = pose.copy();
        if (tz != 0) {
            renderPose.pose().translate(0 , 0 , tz);
        }
        if (rx != 0 || ry != 0) {
            renderPose.pose().rotate(new Quaternionf().rotateXYZ(rx, ry, 0));
        }
        drawQuad(axis, index,  renderPose, vertexConsumer);
    }

    private void drawQuad(int axis, int index, PoseStack.Pose pose, VertexConsumer builder) {
        float[] uv = getUV(index, axis);
        if (uv != null) {
            drawQuad(builder, pose.pose(), uv[0], uv[1],uv[2], uv[3]);
        }
    }

    private float[] getUV(int index, int axis) {
        return switch (axis) {
            case 0 -> new float[]{quadSize * index, 0f, quadSize * (index + 1), quadSize};
            case 1 -> new float[]{quadSize * index, quadSize, quadSize * (index + 1), quadSize * 2};
            case 2 -> new float[]{quadSize * index, quadSize * 2, quadSize * (index + 1), quadSize * 3};
            default -> null;
        };
    }

    private void drawQuad(VertexConsumer builder, Matrix4f matrix, float u1, float v1, float u2, float v2) {
        builder.addVertex(matrix, -0.5f, 0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, BASE_ALPHA).setUv(u2, v2).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
        builder.addVertex(matrix, 0.5f, 0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, BASE_ALPHA).setUv(u1, v2).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
        builder.addVertex(matrix, 0.5f, -0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, BASE_ALPHA).setUv(u1, v1).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
        builder.addVertex(matrix, -0.5f, -0.5f, 0.0F).setColor(1.0F, 1.0F, 1.0F, BASE_ALPHA).setUv(u2, v1).setLight(157288880).setOverlay(OverlayTexture.NO_OVERLAY);
    }
}
