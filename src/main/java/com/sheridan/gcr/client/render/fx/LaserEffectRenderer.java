package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
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

    static {
        Stage.HIGH.addTask(new Task(LaserEffectRenderer::renderEffect).forever());
    }

    /**
     * 新增：通过 currentNodeId 查询射线命中长度的方法
     * 如果找不到节点，默认返回 Float.NaN
     */
    public static float getHitLength(String currentNodeId) {
        NodeState state = activeNodes.get(currentNodeId);
        return state != null ? state.hitLength : Float.NaN;
    }

    public static void recordEffectCall(int color, String currentNodeId, PoseStack.Pose laserSourcePose, ModuleRenderContext context) {
        if (!structureChanged) {
            int modifyID = context.gun.getModifyID(context.itemStack);
            String identityID = context.gun.getIdentityID(context.itemStack);
            if (modifyID != lastModifyID || !Objects.equals(identityID, lastIdentityID)) {
                lastModifyID = modifyID;
                lastIdentityID = identityID;
                structureChanged = true;
            }
        }

        float fovScene = (float) RenderEvents.currentFov;
        float scale = (float) (
                Math.tan(Math.toRadians(fovScene * 0.5)) /
                        Math.tan(Math.toRadians(35))
        );

        Matrix4f poseMatrix = laserSourcePose.pose();
        Vector3f localPos = poseMatrix.getTranslation(new Vector3f());
        Vector3f localDir = poseMatrix.transformDirection(new Vector3f(0, 0, -1)).normalize();
        localDir.mul(scale, scale, 1);
        localPos.mul(0.25f * scale, 0.25f * scale, 0.25f);

        activeNodes.compute(currentNodeId, (id, state) -> {
            if (state == null) {
                state = new NodeState(id);
            }
            state.localPos = localPos;
            state.localDir = localDir;
            state.recorded = true;
            state.color = color;
            return state;
        });
    }

    private static Matrix4f calculateCleanProjectionMatrix() {
        Minecraft mc = Minecraft.getInstance();
        Matrix4f cleanProj = new Matrix4f();
        if (mc.level == null || mc.player == null) {
            return cleanProj.set(RenderSystem.getProjectionMatrix());
        }
        cleanProj.identity();
        float fovRadians = (float)(RenderEvents.currentFov * (Math.PI / 180.0));
        float aspectRatio = (float)mc.getWindow().getWidth() / (float)mc.getWindow().getHeight();
        float nearPlane = 0.05F;
        float farPlane = mc.gameRenderer.getDepthFar();
        cleanProj.perspective(fovRadians, aspectRatio, nearPlane, farPlane);
        return cleanProj;
    }

    static Matrix4f modelViewMat = new Matrix4f();
    static Matrix4f projectionMat = new Matrix4f();

    @SubscribeEvent
    public static void saveMatrix(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (Client.isUseIrisShader) {
                Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                Quaternionf quaternionf = camera.rotation().conjugate(new Quaternionf());
                Matrix4f matrix4f = (new Matrix4f()).rotation(quaternionf);
                modelViewMat.set(matrix4f);
            } else {
                modelViewMat.set(RenderSystem.getModelViewMatrix());
            }
            projectionMat.set(calculateCleanProjectionMatrix());
        }
    }

    public static void renderEffect(RenderLevelStageEvent event) {
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
                    node.hitLength = masterNode.hitLength; // 共享 Master 的命中长度
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

            if (hitEntity || blockHit.getType() != HitResult.Type.MISS) {
                node.lastHitPos = finalHitPos;
                float distance = (float) start.distanceTo(finalHitPos);

                if (distance < 0.75F) {
                    node.hitLength = Float.NaN;
                } else {
                    node.hitLength = distance;
                }
            } else {
                node.lastHitPos = null;
                node.hitLength = 64.0F;
            }
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);

        GL20.glUseProgram(LaserGlowShader.programId);
        GL30.glBindVertexArray(LaserGlowShader.vaoId);

        float[] matBuffer = new float[16];
        projectionMat.get(matBuffer);
        GL20.glUniformMatrix4fv(LaserGlowShader.projMatLoc, false, matBuffer);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMat);
        for (NodeState node : activeNodes.values()) {
            // 条件变更为：有命中坐标，且命中长度不能为 NaN (即长度必须 >= 1.5 block)
            if (node.lastHitPos != null && !Float.isNaN(node.hitLength)) {
                double renderX = node.lastHitPos.x - camPos.x;
                double renderY = node.lastHitPos.y - camPos.y;
                double renderZ = node.lastHitPos.z - camPos.z;

                poseStack.pushPose();
                poseStack.translate(renderX, renderY, renderZ);

                float scale = 0.05F;
                poseStack.scale(scale, scale, scale);
                Matrix4f modelViewMatrix = poseStack.last().pose();
                modelViewMatrix.get(matBuffer);
                GL20.glUniformMatrix4fv(LaserGlowShader.modelViewMatLoc, false, matBuffer);

                int color = node.color;

                int a = (color >> 24) & 0xFF;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (a == 0) a = 255;

                float rF = r / 255.0f;
                float gF = g / 255.0f;
                float bF = b / 255.0f;
                float aF = a / 255.0f;

                GL20.glUniform4f(LaserGlowShader.glowColorLoc, rF, gF, bF, aF);

                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, LaserGlowShader.vertexCount);
                poseStack.popPose();
            }
            node.recorded = false;
        }

        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
        GL11.glDepthMask(true);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

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
        public int color;
        boolean recorded = false;
        // 新增：用于记录当前的命中长度
        float hitLength = Float.NaN;

        NodeState(String id) {
            this.id = id;
        }
    }
}