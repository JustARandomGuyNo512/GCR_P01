package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LaserEffectRenderer {


    private static final Map<String, NodeState> activeNodes = new HashMap<>();

    private static final Map<String, String> slaveToMasterCache = new HashMap<>();

    private static int lastModifyID = -1;
    private static String lastIdentityID = "";
    private static boolean structureChanged = false;

    public static void recordEffectCall(int color, String currentNodeId, PoseStack.Pose laserSourcePose, ModuleRenderContext context) {
        if (!structureChanged) {
            int modifyID = context.gun.getModifyID(context.itemStack);
            String identityID = context.gun.getIdentityID(context.itemStack);
            if (modifyID != lastModifyID || !Objects.equals(identityID, lastIdentityID)) {
                lastModifyID = modifyID;
                lastIdentityID = identityID;
                structureChanged = true; // 标记结构改变，通知渲染端重新计算共线
            }
        }

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
            state.recorded = true;
            return state;
        });
    }

    static Matrix4f modelViewMat = new Matrix4f();
    static Matrix4f projectionMat = new Matrix4f();
    @SubscribeEvent
    public static void saveMatrix(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            modelViewMat.set(RenderSystem.getModelViewMatrix());
            projectionMat.set(RenderSystem.getProjectionMatrix());
        }
    }

    @SubscribeEvent
    public static void renderEffect(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        activeNodes.values().removeIf(state -> !state.recorded);

        if (activeNodes.isEmpty()) {
            slaveToMasterCache.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        List<NodeState> nodes = new ArrayList<>(activeNodes.values());

        if (structureChanged) {
            slaveToMasterCache.clear();
            for (int i = 0; i < nodes.size(); i++) {
                NodeState n1 = nodes.get(i);
                if (slaveToMasterCache.containsKey(n1.id)) {
                    continue;
                }

                for (int j = i + 1; j < nodes.size(); j++) {
                    NodeState n2 = nodes.get(j);
                    if (slaveToMasterCache.containsKey(n2.id)) {
                        continue;
                    }

                    if (isCollinear(n1, n2)) {
                        slaveToMasterCache.put(n2.id, n1.id);
                    }
                }
            }
            structureChanged = false;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Quaternionf cameraRotation = camera.rotation();
        for (NodeState node : nodes) {
            if (slaveToMasterCache.containsKey(node.id)) {
                String masterId = slaveToMasterCache.get(node.id);
                NodeState masterNode = activeNodes.get(masterId);
                if (masterNode != null) {
                    node.lastHitPos = masterNode.lastHitPos;
                    node.hitDirection = masterNode.hitDirection;
                    continue;
                }
            }

            Vector3f worldPosOffset = new Vector3f(node.localPos).rotate(cameraRotation);
            Vector3f worldDir = new Vector3f(node.localDir).rotate(cameraRotation);

            Vec3 start = camPos.add(worldPosOffset.x, worldPosOffset.y, worldPosOffset.z);
            double laserRange = 64.0D;
            Vec3 end = start.add(worldDir.x * laserRange, worldDir.y * laserRange, worldDir.z * laserRange);

            HitResult blockHit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            Vec3 finalHitPos = blockHit.getLocation();

            // 提取方块命中方向
            Direction blockDirection = null;
            if (blockHit.getType() == HitResult.Type.BLOCK && blockHit instanceof BlockHitResult blockHitResult) {
                blockDirection = blockHitResult.getDirection();
            }

            Vec3 entityEnd = blockHit.getType() != HitResult.Type.MISS ? finalHitPos : end;
            double closestDistSq = start.distanceToSqr(entityEnd);

            AABB searchBox = new AABB(start, entityEnd).inflate(1.0D);
            boolean hitEntity = false;

            for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> !e.isSpectator() && e.isPickable())) {
                AABB entityAABB = entity.getBoundingBox().inflate(entity.getPickRadius());
                Optional<Vec3> hitOptional = entityAABB.clip(start, entityEnd);
                if (hitOptional.isPresent()) {
                    double distSq = start.distanceToSqr(hitOptional.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        finalHitPos = hitOptional.get();
                        hitEntity = true;
                    }
                }
            }

            node.lastHitPos = finalHitPos;
            node.hitDirection = hitEntity ? null : blockDirection;
        }


        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        GL20.glUseProgram(LaserGlowShader.programId);
        GL30.glBindVertexArray(LaserGlowShader.vaoId);


        float[] matBuffer = new float[16];
        projectionMat.get(matBuffer);
        GL20.glUniformMatrix4fv(LaserGlowShader.projMatLoc, false, matBuffer);

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.mulPose(modelViewMat);
        for (NodeState node : activeNodes.values()) {
            if (node.lastHitPos != null) {
                double renderX = node.lastHitPos.x - camPos.x;
                double renderY = node.lastHitPos.y - camPos.y;
                double renderZ = node.lastHitPos.z - camPos.z;

                poseStack.translate(renderX, renderY, renderZ);

                float scale = 1F;
                poseStack.scale(scale, scale, scale);
                Matrix4f modelViewMatrix = poseStack.last().pose();
                modelViewMatrix.get(matBuffer);
                GL20.glUniformMatrix4fv(LaserGlowShader.modelViewMatLoc, false, matBuffer);

                GL20.glUniform4f(LaserGlowShader.glowColorLoc, 0.0f, 0.7f, 1.0f, 1.0f);

                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, LaserGlowShader.vertexCount);
            }
            node.recorded = false;
        }


        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
        GL11.glDepthMask(true);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

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

    public static class NodeState {
        final String id;
        Vector3f localPos = new Vector3f();
        Vector3f localDir = new Vector3f();
        Vec3 lastHitPos = null;
        Direction hitDirection = null; // 新增字段：用于记录命中的面方向
        boolean recorded = false; // 代替原来的时间戳戳标记

        NodeState(String id) {
            this.id = id;
        }
    }
}