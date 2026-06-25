package com.sheridan.gcr.client.render.fx.muzzleSmoke.fast;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(Dist.CLIENT)
public class MuzzleSmokeRenderer {
    private static final ByteBufferBuilder DELAYED_TASK_BUFFER = new ByteBufferBuilder(1024);
    private static final Deque<MuzzleSmokeTask> tasks = new ArrayDeque<>();
    public static final int MAX_DELAYED_TASKS = 5;
    public static final MuzzleSmokeRenderer INSTANCE = new MuzzleSmokeRenderer();
    private static long tempLastShoot = 0;

    /**
     * Only call this method on render thread!!!
     * */
    public void pushEffect(FastMuzzleSmoke effect, PoseStack.Pose pose, long lastShoot, int light)  {
        if (effect == null) {
            return;
        }
        if (tempLastShoot != lastShoot) {
            if (tasks.size() > MAX_DELAYED_TASKS) {
                tasks.pollLast();
            }
            if (tasks.size() < MAX_DELAYED_TASKS) {
                PoseStack.Pose renderPose = pose.copy();
                renderPose.pose().translate(0, 0, -0.0125f);
                tasks.offerFirst(new MuzzleSmokeTask(renderPose, lastShoot, effect, light));
            }
            tempLastShoot = lastShoot;
        }
    }

    public void render() {
        if (!tasks.isEmpty()) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(DELAYED_TASK_BUFFER);
            tasks.removeIf((task) -> task.handleRender(bufferSource));
            bufferSource.endBatch();
        }
    }

    public void clearEffects() {
        tasks.clear();
    }

    public boolean hasTask() {
        return !tasks.isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (!INSTANCE.renderImmediate && !tasks.isEmpty() &&
//                event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
//
//            RenderSystem.backupProjectionMatrix();
//            RenderSystem.setProjectionMatrix(Client.FIRST_PERSON_PROJECTION_MAT, VertexSorting.DISTANCE_TO_ORIGIN);
//            if (tempModelViewMatrix != null) {
//                RenderSystem.getModelViewStack().pushMatrix();
//                RenderSystem.getModelViewStack().set(tempModelViewMatrix);
//                RenderSystem.applyModelViewMatrix();
//            }
//            depthMask = true;
//            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(DELAYED_TASK_BUFFER);
//            tasks.removeIf((task) -> task.handleRender(bufferSource));
//            bufferSource.endBatch();
//            RenderSystem.restoreProjectionMatrix();
//            if (tempModelViewMatrix != null) {
//                RenderSystem.getModelViewStack().popMatrix();
//                RenderSystem.applyModelViewMatrix();
//            }
//        }
    }

//    @SubscribeEvent
//    public static void onRenderHandLast(RenderHandEvent event) {
//        if (INSTANCE.renderImmediate && event.getHand() == InteractionHand.MAIN_HAND) {
//            depthMask = true;
//            if (tempModelViewMatrix != null) {
//                RenderSystem.getModelViewStack().pushMatrix();
//                RenderSystem.getModelViewStack().set(tempModelViewMatrix);
//                RenderSystem.applyModelViewMatrix();
//            }
//            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(DELAYED_TASK_BUFFER);
//            tasks.removeIf((task) -> task.handleRender(bufferSource));
//            bufferSource.endBatch();
//            depthMask = true;
//            if (tempModelViewMatrix != null) {
//                RenderSystem.getModelViewStack().popMatrix();
//                RenderSystem.applyModelViewMatrix();
//            }
//        }
//    }
}
