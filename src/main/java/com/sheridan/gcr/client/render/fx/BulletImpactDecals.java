package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Lightweight, client-only wall decals. No entities, chunks or saved data.
 * Bounded to 256 marks per world; each mark expires after 90 seconds.
 */
@OnlyIn(Dist.CLIENT)
public final class BulletImpactDecals {
    private static final RenderType RENDER_TYPE =
            RenderType.entityTranslucent(GCR.RL("textures/particle/bullet_hole.png"));
    private static final int MAX_MARKS = 256;
    private static final long LIFETIME_TICKS = 20L * 90L;
    private static final long FADE_TICKS = 20L * 10L;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;

    private static final Deque<Decal> MARKS = new ArrayDeque<>();
    private static ClientLevel previousLevel;

    private BulletImpactDecals() {}

    public static void record(Vec3 impact, Direction face) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || face == null) return;
        ensureWorld(mc.level);
        if (MARKS.size() >= MAX_MARKS) MARKS.removeFirst();
        MARKS.addLast(new Decal(impact, Vec3.atLowerCornerOf(face.getNormal()), mc.level.getGameTime()));
    }

    private static void ensureWorld(ClientLevel level) {
        if (level != previousLevel) {
            previousLevel = level;
            MARKS.clear();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        ensureWorld(mc.level);
        if (mc.level == null || MARKS.isEmpty()) return;

        long now = mc.level.getGameTime();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
        Matrix4f poseMatrix = event.getPoseStack().last().pose();
        boolean drawn = false;

        for (Iterator<Decal> it = MARKS.iterator(); it.hasNext();) {
            Decal mark = it.next();
            long age = now - mark.birthTick;
            if (age < 0 || age >= LIFETIME_TICKS || !isBackingBlockPresent(mc.level, mark)) {
                it.remove();
                continue;
            }
            if (mark.position.distanceToSqr(cameraPos) > MAX_DISTANCE_SQR) continue;
            int alpha = age > LIFETIME_TICKS - FADE_TICKS
                    ? (int) (220f * (LIFETIME_TICKS - age) / FADE_TICKS) : 220;
            drawDecal(consumer, poseMatrix, mark, cameraPos, alpha);
            drawn = true;
        }
        if (drawn) buffers.endBatch(RENDER_TYPE);
    }

    private static boolean isBackingBlockPresent(ClientLevel level, Decal mark) {
        // Probe inside the original face; do not leave decals floating if a
        // wall is broken, while avoiding any persistent blockstate references.
        BlockPos behind = BlockPos.containing(mark.position.subtract(mark.normal.scale(0.02)));
        return !level.getBlockState(behind).isAir();
    }

    private static void drawDecal(VertexConsumer out, Matrix4f matrix, Decal mark,
                                  Vec3 cameraPos, int alpha) {
        Vector3f n = mark.normal.toVector3f().normalize();
        Vector3f reference = Math.abs(n.y) > 0.98f
                ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f right = reference.cross(n, new Vector3f()).normalize();
        Vector3f up = n.cross(right, new Vector3f()).normalize();

        // Stable per-mark orientation breaks up repeated texture patterns.
        double rotation = mark.rotation;
        Vector3f a = new Vector3f(right).mul((float) Math.cos(rotation))
                .add(new Vector3f(up).mul((float) Math.sin(rotation))).mul(0.065f);
        Vector3f b = new Vector3f(up).mul((float) Math.cos(rotation))
                .sub(new Vector3f(right).mul((float) Math.sin(rotation))).mul(0.065f);
        Vec3 at = mark.position.add(mark.normal.scale(0.006)).subtract(cameraPos);
        vertex(out, matrix, at, a, b, -1, -1, 0, 1, alpha);
        vertex(out, matrix, at, a, b,  1, -1, 1, 1, alpha);
        vertex(out, matrix, at, a, b,  1,  1, 1, 0, alpha);
        vertex(out, matrix, at, a, b, -1,  1, 0, 0, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 position,
                               Vector3f x, Vector3f y, float signX, float signY,
                               float u, float v, int alpha) {
        out.vertex(matrix,
                (float) position.x + x.x * signX + y.x * signY,
                (float) position.y + x.y * signX + y.y * signY,
                (float) position.z + x.z * signX + y.z * signY)
                .color(255, 255, 255, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0, 0, 1).endVertex();
    }

    private static final class Decal {
        final Vec3 position;
        final Vec3 normal;
        final long birthTick;
        final double rotation;

        Decal(Vec3 position, Vec3 normal, long birthTick) {
            this.position = position;
            this.normal = normal;
            this.birthTick = birthTick;
            long hash = Double.doubleToLongBits(position.x * 7.71
                    + position.y * 19.13 + position.z * 2.93);
            this.rotation = ((hash >>> 12) & 1023) * (Math.PI * 2.0 / 1024.0);
        }
    }
}
