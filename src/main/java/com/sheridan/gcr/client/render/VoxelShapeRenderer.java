package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.modularSys.IVoxel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class VoxelShapeRenderer {
    public static void renderRed(IVoxel voxel, Matrix4f pos, MultiBufferSource bufferSource) {
        render(voxel, pos, bufferSource, 1, 0, 0, 1);
    }

    public static void renderWhite(IVoxel voxel, Matrix4f pos, MultiBufferSource bufferSource) {
        render(voxel, pos, bufferSource, 1, 1, 1, 1);
    }

    public static void renderYellow(IVoxel voxel, Matrix4f pos, MultiBufferSource bufferSource) {
        render(voxel, pos, bufferSource, 1, 1, 0, 1);
    }

    public static void renderGreen(IVoxel voxel, Matrix4f pos, MultiBufferSource bufferSource) {
        render(voxel, pos, bufferSource, 0, 1, 0, 1);
    }

    public static void renderBlue(IVoxel voxel, Matrix4f pos, MultiBufferSource bufferSource) {
        render(voxel, pos, bufferSource, 0, 0, 1, 1);
    }

    public static void render(IVoxel voxel, Matrix4f pos, MultiBufferSource bufferSource, float r, float g, float b, float a) {
        List<AABB> innerShape = voxel.getInnerShape();
        if (innerShape.isEmpty()) {
            return;
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.LINES);
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(pos);
        for (AABB aabb : innerShape) {
            LevelRenderer.renderLineBox(poseStack, buffer, aabb, r, g, b, a);
        }
    }

    public static void renderPivot(Matrix4f mat, Vector3f pos, int color, MultiBufferSource bufferSource) {
        renderPivots(mat, List.of(Pair.of(pos, color)), bufferSource);
    }

    public static void renderPivots(Matrix4f pos, List<Pair<Vector3f, Integer>> pivotsAndColor, MultiBufferSource bufferSource) {
        if (pivotsAndColor.isEmpty()) {
            return;
        }
        Matrix4f renderPos = new Matrix4f();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.LINES);
        for (Pair<Vector3f, Integer> pair : pivotsAndColor) {
            Vector3f pivot = pair.getKey();
            Integer color = pair.getRight();
            renderPos.identity();
            renderPos.set(pos);
            renderPos.translate(pivot.x, pivot.y, pivot.z);

            float len = 0.25f;

            float a = ((color >> 24) & 0xFF) / 255.0f;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            // X 轴
            buffer.addVertex(renderPos, -len, 0, 0).setColor(r, g, b, a).setNormal(1, 0, 0);
            buffer.addVertex(renderPos, +len, 0, 0).setColor(r, g, b, a).setNormal(1, 0, 0);

            // Y 轴
            buffer.addVertex(renderPos, 0, -len, 0).setColor(r, g, b, a).setNormal(0, 1, 0);
            buffer.addVertex(renderPos, 0, +len, 0).setColor(r, g, b, a).setNormal(0, 1, 0);

            // Z 轴
            buffer.addVertex(renderPos, 0, 0, -len).setColor(r, g, b, a).setNormal(0, 0, 1);
            buffer.addVertex(renderPos, 0, 0, +len).setColor(r, g, b, a).setNormal(0, 0, 1);
        }

    }
}
