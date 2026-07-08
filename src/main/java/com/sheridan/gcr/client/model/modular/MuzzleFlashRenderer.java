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
import org.joml.Vector3f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public final class MuzzleFlashRenderer implements IMuzzleFlashRenderer{
    public static final int RENDER_CANCELED = 10000;
    private final Map<String, MuzzleEntry> entryMap = new HashMap<>();
    private final List<MuzzleEntry> entries = new ArrayList<>();

    public static final List<Triple<MuzzleEntry, PoseStack.Pose, Long>> MUZZLE_FLASH_QUEUE = new ArrayList<>();
    public static final Map<String, SmokeTasks> MUZZLE_SMOKE_TASKS = new HashMap<>();
    public static final int MAX_SMOKE_EFFECT_TASKS = 5;
    private static final Vector3f DISTANCE_SORTING = new Vector3f();
    private static final List<RenderEntry> UNIFIED_RENDER_QUEUE = new ArrayList<>();
    // 对象池，避免每帧 new RenderEntry
    private static final ArrayDeque<RenderEntry> ENTRY_POOL = new ArrayDeque<>();

    private static final class RenderEntry {
        float z;
        byte type; // 0 = smoke, 1 = flash
        MuzzleSmokeTask smokeTask;
        MuzzleEntry muzzleEntry;
        PoseStack.Pose bonePose;
        long startTime;

        void setSmoke(float z, MuzzleSmokeTask task) {
            this.z = z; this.type = 0; this.smokeTask = task;
        }
        void setFlash(float z, MuzzleEntry entry, PoseStack.Pose pose, long time) {
            this.z = z; this.type = 1; this.muzzleEntry = entry; this.bonePose = pose; this.startTime = time;
        }
    }


    private static RenderEntry acquireEntry() {
        RenderEntry e = ENTRY_POOL.poll();
        return e != null ? e : new RenderEntry();
    }

    public static class SmokeTasks{
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
                if (context.entity.getId() != Client.LOCAL_PLAYER_ID) {
                    return;
                }
                MuzzleFlash muzzleFlash = entry.getMuzzleFlash();
                if (System.currentTimeMillis() - startTime <= muzzleFlash.length) {
                    MUZZLE_FLASH_QUEUE.add(Triple.of(entry, bonePose, startTime));
                    Client.WEAPON_STATUS.setMuzzleFlashConfig(bonePose, entry.flashLightIntensity * (0.95f + 0.1f * Client.WEAPON_STATUS.shootRandomSeed));
                }
                FastMuzzleSmoke muzzleSmoke = entry.getMuzzleSmoke();
                if (muzzleSmoke != null) {
                    String id = context.currentRenderNode().id + entry.getName();
                    SmokeTasks tasks = MUZZLE_SMOKE_TASKS.get(id);
                    if (tasks == null) {
                        tasks = new SmokeTasks(startTime);
                        tasks.queue.add(new MuzzleSmokeTask(bonePose.copy(), startTime, muzzleSmoke, context.light, entry.getSmokeScale()));
                        MUZZLE_SMOKE_TASKS.put(id, tasks);
                    } else {
                        if (tasks.lastCall != startTime) {
                            if (tasks.queue.size() > MAX_SMOKE_EFFECT_TASKS) {
                                tasks.queue.pollLast();
                            }
                            if (tasks.queue.size() < MAX_SMOKE_EFFECT_TASKS) {
                                PoseStack.Pose renderPose = bonePose.copy();
                                renderPose.pose().translate(0, 0, -0.015f);
                                tasks.queue.offerFirst(new MuzzleSmokeTask(renderPose, startTime, muzzleSmoke, context.light, entry.getSmokeScale()));
                            }
                            tasks.lastCall = startTime;
                        }
                    }
                }
            } else if (context.isThirdPerson()) {
                entry.getMuzzleFlash().render(bonePose, context.bufferSource, entry.getScale(), startTime, false, LightTexture.FULL_BRIGHT);
            }
        }
    }

    public static void renderAllFirstPerson(MultiBufferSource bufferSource) {
        // ── 1. 收集 Smoke ────────────────────────────────────────────────
        if (!MUZZLE_SMOKE_TASKS.isEmpty()) {
            // 直接用 entrySet 迭代器删除，省掉 idToRemove Set 分配
            Iterator<Map.Entry<String, SmokeTasks>> mapIt = MUZZLE_SMOKE_TASKS.entrySet().iterator();
            while (mapIt.hasNext()) {
                SmokeTasks tasks = mapIt.next().getValue();

                Iterator<MuzzleSmokeTask> it = tasks.queue.iterator();
                while (it.hasNext()) {
                    MuzzleSmokeTask task = it.next();
                    if (task.isFinished()) {
                        it.remove();
                        continue;
                    }
                    float z = -task.pose.pose().getTranslation(DISTANCE_SORTING).z;
                    RenderEntry re = acquireEntry();
                    re.setSmoke(z, task);
                    UNIFIED_RENDER_QUEUE.add(re);
                }

                if (tasks.queue.isEmpty()) {
                    mapIt.remove();
                }
            }
        }

        // ── 2. 收集 Flash ────────────────────────────────────────────────
        for (Triple<MuzzleEntry, PoseStack.Pose, Long> pair : MUZZLE_FLASH_QUEUE) {
            PoseStack.Pose bonePose = pair.getMiddle();
            float z = -bonePose.pose().getTranslation(DISTANCE_SORTING).z;
            RenderEntry re = acquireEntry();
            re.setFlash(z, pair.getLeft(), bonePose, pair.getRight());
            UNIFIED_RENDER_QUEUE.add(re);
        }

        // ── 3. 排序 ──────────────────────────────────────────────────────
        if (UNIFIED_RENDER_QUEUE.size() > 1) {
            UNIFIED_RENDER_QUEUE.sort((a, b) -> Float.compare(b.z, a.z));
        }

        // ── 4. 渲染 ──────────────────────────────────────────────────────
        for (RenderEntry re : UNIFIED_RENDER_QUEUE) {
            if (re.type == 0) {
                re.smokeTask.handleRender(bufferSource);
            } else {
                re.muzzleEntry.getMuzzleFlash()
                        .render(re.bonePose, bufferSource, re.muzzleEntry.getScale(),
                                re.startTime, true, LightTexture.FULL_BRIGHT);
            }
            // 归还对象池
            re.smokeTask = null;
            re.muzzleEntry = null;
            re.bonePose = null;
            ENTRY_POOL.offer(re);
        }

        // ── 5. 清理 ──────────────────────────────────────────────────────
        UNIFIED_RENDER_QUEUE.clear();
        MUZZLE_FLASH_QUEUE.clear();
    }

    @Override
    public void onAfterAllRendered(ModuleRenderContext context) {
        if (!context.isFirstPerson() || context.getLocalStorage(RENDER_CANCELED) != null) {
            return;
        }
        if (context.entity == null || context.entity.getId() != Client.LOCAL_PLAYER_ID) {
            return;
        }
        if (Client.isUseIrisShader) {
            if (IrisCompat.isRenderingShadowPass()) {
                return;
            }
            final Matrix4f modelViewMat = Client.getGunRenderer().firstPersonModelViewMat();
            Stage.LOW.addTask(new Task((RenderLevelStageEvent event) -> deferredRender(modelViewMat)));
        } else {
            renderAllFirstPerson(context.bufferSource);
        }
        context.setLocalStorage(RENDER_CANCELED, 1);
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
