package com.sheridan.gcr.client.render.fx.bulletShell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.bulletShell.BulletShellModel;
import com.sheridan.gcr.compat.IrisCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class BulletShellRenderer {
    public static final int MAX_SIZE = 20;
    private static final Set<BulletShellModel> MODELS = new HashSet<>();
    private static final PoseStack POSE_STACK = new PoseStack();
    private static final Vector3f TEMP_OFFSET_VEC = new Vector3f();
    private static final Quaternionf TEMP_ROT_QUAT = new Quaternionf();

    private static class Task {
        public final BulletShellDisplay display;
        public final BulletShellModel model;
        public final PoseStack.Pose initialPose;
        public final long timeStamp;

        public final float velocity;
        public final Vector3f direction;
        public final float rotXOffset;
        public final float rotYOffset;
        public final Vector2f rotateSpeed;

        Task(BulletShellDisplay display, BulletShellModel model, PoseStack.Pose pose, long timeStamp) {
            this.display = display;
            this.model = model;
            this.initialPose = pose;
            this.timeStamp = timeStamp;

            float dirY = (float) (display.front + display.frontRandomDeg * Math.random());
            float dirZ = (float) (display.up + display.upRandomDeg * (Math.random() - 0.5f));
            Vector3f baseDir = new Vector3f(1, 0, 0);
            baseDir.rotateY((float) Math.toRadians(dirY));
            baseDir.rotateZ((float) Math.toRadians(dirZ));
            this.direction = baseDir.normalize();

            rotXOffset = (float) (display.rotatePitchRandomOffset * (Math.random() - 0.5f));
            rotYOffset = (float) (display.rotateYawRandomOffset * (Math.random() - 0.5f));

            float vRand = 1.0f + (float) (Math.random() * 2 - 1) * display.velocityRandom;
            this.velocity = display.velocity * vRand;

            this.rotateSpeed = new Vector2f(
                    (float) (display.rotateYawSpeed * (1 + Math.random() * display.rotateYawSpeedRandom)),
                    (float) (display.rotatePitchSpeed * (1 + (Math.random() - 0.5f) * display.rotatePitchSpeedRandom)));
        }

        boolean isAlive(long now) {
            return now - timeStamp < display.lifeTime;
        }

        float age(long now) {
            return now - timeStamp;
        }
    }

    private static final Deque<Task> tasks = new ArrayDeque<>();

    public static void renderInFirstPerson(int lightmapUV) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        long now = System.currentTimeMillis();

        tasks.removeIf(t -> !t.isAlive(now));

        for (Task t : tasks) {
            float age = t.age(now);

            float dt = age * 0.001f;

            float drop = t.display.drop * dt * dt * 0.5f;

            TEMP_OFFSET_VEC.set(t.direction).mul(t.velocity * dt);
            TEMP_OFFSET_VEC.y -= drop;

            POSE_STACK.pushPose();
            POSE_STACK.last().pose().mul(t.initialPose.pose());
            POSE_STACK.translate(TEMP_OFFSET_VEC.x, TEMP_OFFSET_VEC.y, TEMP_OFFSET_VEC.z);

            TEMP_ROT_QUAT.identity().rotateXYZ(
                    t.rotateSpeed.y * dt + t.rotXOffset,
                    t.rotateSpeed.x * dt + t.rotYOffset,
                    0
            );
            POSE_STACK.mulPose(TEMP_ROT_QUAT);

            t.model.addInstance(POSE_STACK.last(), lightmapUV);
            MODELS.add(t.model);
            POSE_STACK.popPose();
        }
        for (BulletShellModel model : MODELS) {
            model.render(true);
        }
        MODELS.clear();
    }

    public static void push(BulletShellDisplay display, BulletShellModel model, PoseStack.Pose initialPose, long timeStamp) {
        if (tasks.size() >= MAX_SIZE) {
            tasks.removeLast();
        }
        tasks.addFirst(new Task(display, model, initialPose, timeStamp));
    }

    public static void clear() {
        tasks.clear();
    }
}
