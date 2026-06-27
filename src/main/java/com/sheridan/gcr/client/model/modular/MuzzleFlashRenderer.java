package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import com.sheridan.gcr.client.render.fx.muzzleFlash.MuzzleFlash;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.fast.MuzzleSmokeTask;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public final class MuzzleFlashRenderer implements IMuzzleFlashRenderer{
    public static final int RENDER_CANCELED = 10000;
    private final Map<String, MuzzleEntry> entryMap = new HashMap<>();
    private final List<MuzzleEntry> entries = new ArrayList<>();

    private static final List<Triple<MuzzleEntry, PoseStack.Pose, Long>> renderQueue = new ArrayList<>();
    private static final Map<String, SmokeTasks> muzzleSmokes = new HashMap<>();
    public static final int MAX_SMOKE_EFFECT_TASKS = 5;

    private static class SmokeTasks{
        public long lastCall;
        public Deque<MuzzleSmokeTask> queue;

        public SmokeTasks(long lastCall) {
            this.lastCall = lastCall;
            queue = new ArrayDeque<>();
        }
    }

    public MuzzleFlashRenderer(MuzzleEntry ... entries) {
        this.entries.addAll(List.of(entries));
        for (MuzzleEntry entry : entries) {
            entryMap.put(entry.getName(), entry);
        }
    }

    @Override
    public @NotNull List<MuzzleEntry> getMuzzleFlashEntries() {
        return entries;
    }

    @Override
    public @Nullable MuzzleEntry getByName(String name) {
        return entryMap.get(name);
    }

    @Override
    public void onRender(ModuleRenderContext context, IMuzzleFlashRendererModel model, GunEffect effectListener, String effectModuleId) {
        recordOrRender(context, model, effectListener, effectModuleId);
    }


    public void recordOrRender(ModuleRenderContext context, IMuzzleFlashRendererModel model, GunEffect effectListener, String effectModuleId) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        if (context.entity == null) {
            return;
        }
        for (MuzzleEntry entry : entries) {
            if (!entry.enabled) {
                continue;
            }
            String bindSlotName = entry.getBindSlotName();
            ModuleRenderNode node = context.currentRenderNode();
            if (node.hasChild(bindSlotName)) {
                continue;
            }
            PoseStack.Pose bonePose = model.getBonePose(entry.getBoneName());
            if (bonePose == null) {
                continue;
            }
            long startTime = GunEffectManager.getEffectTimestamp(
                    context.entity.getId(),
                    effectListener,
                    effectModuleId
            );
            if (startTime == -1) {
                return;
            }
            if (context.isFirstPerson()) {
                MuzzleFlash muzzleFlash = entry.getMuzzleFlash();
                if (System.currentTimeMillis() - startTime > muzzleFlash.length) {
                    renderQueue.add(Triple.of(entry, bonePose, startTime));
                }
                String id = context.currentRenderNode().id + entry.getName();
                SmokeTasks tasks = muzzleSmokes.get(id);
                if (tasks == null) {
                    tasks = new SmokeTasks(startTime);
                    tasks.queue.add(new MuzzleSmokeTask(bonePose.copy(), startTime, entry.getMuzzleSmoke(), context.light));
                    muzzleSmokes.put(id, tasks);
                } else {
                    if (tasks.lastCall != startTime) {
                        if (tasks.queue.size() > MAX_SMOKE_EFFECT_TASKS) {
                            tasks.queue.pollLast();
                        }
                        if (tasks.queue.size() < MAX_SMOKE_EFFECT_TASKS) {
                            PoseStack.Pose renderPose = bonePose.copy();
                            renderPose.pose().translate(0, 0, -0.015f);
                            tasks.queue.offerFirst(new MuzzleSmokeTask(renderPose, startTime, entry.getMuzzleSmoke(), context.light));
                        }
                        tasks.lastCall = startTime;
                    }
                }

            } else if (context.isThirdPerson()) {
                entry.getMuzzleFlash().render(bonePose, context.bufferSource, entry.getScale(), startTime, false, LightTexture.FULL_BRIGHT);
            }
        }
    }


    public static void renderAllFirstPerson(MultiBufferSource bufferSource) {
        for (Triple<MuzzleEntry, PoseStack.Pose, Long> pair : renderQueue) {
            MuzzleEntry entry = pair.getLeft();
            PoseStack.Pose bonePose = pair.getMiddle();
            long startTime = pair.getRight();
            Client.WEAPON_STATUS.setMuzzleFlashPos(bonePose);
            entry.getMuzzleFlash().render(
                    bonePose,
                    bufferSource,
                    entry.getScale(),
                    startTime,
                    true,
                    LightTexture.FULL_BRIGHT);
            FastMuzzleSmoke muzzleSmoke = entry.getMuzzleSmoke();
            muzzleSmoke.render(startTime, bonePose, bufferSource, (int) (Math.random() * 1000), LightTexture.FULL_BRIGHT);
        }
        renderQueue.clear();
        if (muzzleSmokes.isEmpty()) {
            return;
        }
        Set<String> idToRemove = null;
        for (Map.Entry<String, SmokeTasks> entry : muzzleSmokes.entrySet()) {
            String key = entry.getKey();
            SmokeTasks tasks = entry.getValue();
            if (!tasks.queue.isEmpty()) {
                tasks.queue.removeIf((task) -> task.handleRender(bufferSource));
            }
            if (tasks.queue.isEmpty()) {
                if (idToRemove == null) {
                    idToRemove = new HashSet<>();
                }
                idToRemove.add(key);
            }
        }
        if (idToRemove != null) {
            for (String id : idToRemove) {
                muzzleSmokes.remove(id);
            }
        }
    }

    @Override
    public void onAfterAllRendered(ModuleRenderContext context) {
        if (context.getLocalStorage(RENDER_CANCELED) != null) {
            return;
        }
        if (Client.isIrisShaderInUse) {
            if (IrisCompat.isRenderingShadowPass()) {
                return;
            }
            final Matrix4f modelViewMat = Client.getGunRenderer().firstPersonModelViewMat();
            Stage.LOW.addTask(new Task((RenderLevelStageEvent event) -> deferredRender(modelViewMat)));
        } else {
            renderAllFirstPerson(context.bufferSource);
        }
    }

    private void deferredRender(Matrix4f modelViewMat) {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(Client.FIRST_PERSON_PROJECTION_MAT, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewMatrix().set(modelViewMat);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.enableDepthTest();
        renderAllFirstPerson(bufferSource);
        bufferSource.endBatch();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

}
