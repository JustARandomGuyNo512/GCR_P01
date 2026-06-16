package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LaserEffectRenderer {

    private static final Map<String, NodeState> activeNodes = new HashMap<>();

    // Debug 用的假实体，不会真正生成在世界中
    private static Chicken debugChicken;


    public static void recordEffectCall(int color, String currentNodeId, PoseStack.Pose laserSourcePose, ModuleRenderContext context) {
        long now = System.currentTimeMillis();

        Matrix4f poseMatrix = laserSourcePose.pose();
        Vector3f localPos = poseMatrix.transformPosition(new Vector3f(0, 0, 0));

        Vector3f localDir = poseMatrix.transformDirection(new Vector3f(0, 0, -1)).normalize();
        localPos.mul(0.1f);
        activeNodes.compute(currentNodeId, (id, state) -> {
            if (state == null) {
                state = new NodeState(id);
            }
            state.localPos = localPos;
            state.localDir = localDir;
            state.lastRecordedTime = now;
            return state;
        });
    }


    @SubscribeEvent
    public static void renderEffect(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        long now = System.currentTimeMillis();

        activeNodes.values().removeIf(state -> now - state.lastRecordedTime > 100);
        if (activeNodes.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Quaternionf cameraRotation = camera.rotation();

        List<NodeState> nodes = new ArrayList<>(activeNodes.values());
        Map<NodeState, NodeState> slaveToMasterMap = new HashMap<>();

        // 2. 共线优化 (Collinear Check)
        for (int i = 0; i < nodes.size(); i++) {
            NodeState n1 = nodes.get(i);
            if (slaveToMasterMap.containsKey(n1)) continue;

            for (int j = i + 1; j < nodes.size(); j++) {
                NodeState n2 = nodes.get(j);
                if (slaveToMasterMap.containsKey(n2)) continue;

                if (isCollinear(n1, n2)) {
                    slaveToMasterMap.put(n2, n1);
                }
            }
        }


        for (NodeState node : nodes) {
            if (slaveToMasterMap.containsKey(node)) {
                node.lastHitPos = slaveToMasterMap.get(node).lastHitPos;
                continue;
            }

            Vector3f worldPosOffset = new Vector3f(node.localPos).rotate(cameraRotation);
            Vector3f worldDir = new Vector3f(node.localDir).rotate(cameraRotation);

            Vec3 start = camPos.add(worldPosOffset.x, worldPosOffset.y, worldPosOffset.z);
            double laserRange = 64.0D;
            Vec3 end = start.add(worldDir.x * laserRange, worldDir.y * laserRange, worldDir.z * laserRange);

            HitResult blockHit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            Vec3 finalHitPos = blockHit.getLocation();

            Vec3 entityEnd = blockHit.getType() != HitResult.Type.MISS ? finalHitPos : end;
            double closestDistSq = start.distanceToSqr(entityEnd);

            AABB searchBox = new AABB(start, entityEnd).inflate(1.0D);

            for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> !e.isSpectator() && e.isPickable())) {
                AABB entityAABB = entity.getBoundingBox().inflate(entity.getPickRadius());
                Optional<Vec3> hitOptional = entityAABB.clip(start, entityEnd);
                if (hitOptional.isPresent()) {
                    double distSq = start.distanceToSqr(hitOptional.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        finalHitPos = hitOptional.get();
                    }
                }
            }
            node.lastHitPos = finalHitPos;
        }

        // 4. Debug 渲染
        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        if (debugChicken == null || debugChicken.level() != mc.level) {
            debugChicken = new Chicken(EntityType.CHICKEN, mc.level);
        }

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        float partialTick = event.getPartialTick().getRealtimeDeltaTicks();

        for (NodeState node : nodes) {
            double renderX = 0;
            double renderY = 0;
            double renderZ = 0;
            if (node.lastHitPos != null) {
                renderX = node.lastHitPos.x - camPos.x;
                renderY = node.lastHitPos.y - camPos.y;
                renderZ = node.lastHitPos.z - camPos.z;
            }

            // ---------- Debug 渲染 2: 在命中点渲染鸡 ----------
            if (node.lastHitPos != null) {
                poseStack.pushPose();

                poseStack.translate(renderX, renderY, renderZ);
                poseStack.scale(0.25f, 0.25f, 0.25f);
                dispatcher.render(debugChicken, 0, 0, 0, 0, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);

                poseStack.popPose();
            }
        }

        bufferSource.endBatch();
    }

    /**
     * 判断两个节点是否在一条射线上
     */
    private static boolean isCollinear(NodeState n1, NodeState n2) {
        float dotDir = n1.localDir.dot(n2.localDir);
        if (dotDir < 0.99f) {
            return false;
        }


        Vector3f offset = new Vector3f(n2.localPos).sub(n1.localPos);
        float distance = offset.length();
        if (distance < 0.01f) {
            return true;
        }

        offset.normalize();
        float dotOffset = Math.abs(offset.dot(n1.localDir));
        return dotOffset > 0.99f;
    }


    private static class NodeState {
        final String id;
        Vector3f localPos = new Vector3f();
        Vector3f localDir = new Vector3f();
        Vec3 lastHitPos = null;
        long lastRecordedTime = 0;

        NodeState(String id) {
            this.id = id;
        }
    }
}