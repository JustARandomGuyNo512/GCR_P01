package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class LaserEffectRenderer {

    // 默认 50 TPS (约 20 ms)
    private static int rayHitUpdateDelayMs;

    // 存储每个射击节点的上下文状态
    private static final Map<String, NodeState> activeNodes = new HashMap<>();

    // Debug 用的假实体，不会真正生成在世界中
    private static Chicken debugChicken;

    public static void init() {
        Stage.HIGH.addTask(new Task(LaserEffectRenderer::renderEffect).forever());
        setRayHitPosUpdateDelay(20);
    }

    public static void recordEffectCall(String currentNodeId, PoseStack.Pose laserSourcePose, ModuleRenderContext context) {
        long now = System.currentTimeMillis();

        // 从第一人称 PoseStack 中提取相机相对的本地坐标和方向
        Matrix4f poseMatrix = laserSourcePose.pose();
        Vector3f localPos = poseMatrix.transformPosition(new Vector3f(0, 0, 0));
        // 假设模型向前的方向是 -Z (Minecraft 标准)
        Vector3f localDir = poseMatrix.transformDirection(new Vector3f(0, 0, -1)).normalize();

        activeNodes.compute(currentNodeId, (id, state) -> {
            if (state == null) {
                state = new NodeState(id);
                // 核心：初次注册时给一个随机的时间偏移量，使得多个节点错开更新，避免同一帧大量 Raycast
                state.nextUpdateTime = now + (long) (Math.random() * rayHitUpdateDelayMs);
            }
            state.localPos = localPos;
            state.localDir = localDir;
            state.lastRecordedTime = now;
            return state;
        });
    }

    public static void setRayHitPosUpdateDelay(int millSeconds) {
        rayHitUpdateDelayMs = millSeconds;
    }

    public static void renderEffect(RenderLevelStageEvent event) {
        long now = System.currentTimeMillis();
        // 1. 清理过期节点 (100ms 内没有再被 record 的节点认为已停止射击)
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


        List<NodeState> nodes = new ArrayList<>(activeNodes.values());
        Map<NodeState, NodeState> slaveToMasterMap = new HashMap<>();

        // 2. 共线优化 (Collinear Check)
        for (int i = 0; i < nodes.size(); i++) {
            NodeState n1 = nodes.get(i);
            if (slaveToMasterMap.containsKey(n1)) {
                continue;
            }

            for (int j = i + 1; j < nodes.size(); j++) {
                NodeState n2 = nodes.get(j);
                if (slaveToMasterMap.containsKey(n2)){
                    continue;
                }

                if (isCollinear(n1, n2)) {
                    slaveToMasterMap.put(n2, n1); // n2 绑定到 n1，共享 hit 结果
                }
            }
        }

        // 3. 执行 Raycast (按需更新)
        for (NodeState node : nodes) {
            if (slaveToMasterMap.containsKey(node)) {
                // 如果是共线附庸节点，直接白嫖主节点的结果
                node.lastHitPos = slaveToMasterMap.get(node).lastHitPos;
                continue;
            }

            // 主节点执行 Raycast 检查
            if (now >= node.nextUpdateTime) {
                node.nextUpdateTime = now + rayHitUpdateDelayMs;

                // 计算世界坐标 = 相机坐标 + 相机相对坐标
                Vec3 start = camPos.add(node.localPos.x, node.localPos.y, node.localPos.z);
                double laserRange = 64.0D; // 激光最大距离
                Vec3 end = start.add(node.localDir.x * laserRange, node.localDir.y * laserRange, node.localDir.z * laserRange);

                HitResult hitResult = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                node.lastHitPos = hitResult.getLocation();
            }
        }

        // 4. Debug 渲染：在命中点渲染鸡
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        if (debugChicken == null || debugChicken.level() != mc.level) {
            debugChicken = new Chicken(EntityType.CHICKEN, mc.level);
        }

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        float partialTick = event.getPartialTick().getRealtimeDeltaTicks();

        for (NodeState node : nodes) {
            if (node.lastHitPos != null) {
                poseStack.pushPose();

                // 将 PoseStack 移动到命中点相对于相机的位置
                double renderX = node.lastHitPos.x - camPos.x;
                double renderY = node.lastHitPos.y - camPos.y;
                double renderZ = node.lastHitPos.z - camPos.z;
                poseStack.translate(renderX, renderY, renderZ);

                // 强制全亮渲染，避免卡在墙里导致鸡是黑色的
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
        // 1. 判断方向是否一致 (点积接近 1)
        float dotDir = n1.localDir.dot(n2.localDir);
        if (dotDir < 0.99f) {
            return false;
        }

        // 2. 判断两个原点之间的连线是否与射击方向平行
        Vector3f offset = new Vector3f(n2.localPos).sub(n1.localPos);
        float distance = offset.length();
        if (distance < 0.01f) {
            return true; // 距离极近视作同源
        }

        offset.normalize();
        float dotOffset = Math.abs(offset.dot(n1.localDir));
        return dotOffset > 0.99f; // 如果两点连线和射击方向基本重合，则视为共线
    }

    // 内部状态类
    private static class NodeState {
        final String id;
        Vector3f localPos = new Vector3f();
        Vector3f localDir = new Vector3f();
        Vec3 lastHitPos = null;
        long nextUpdateTime = 0;
        long lastRecordedTime = 0;

        NodeState(String id) {
            this.id = id;
        }
    }
}