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
import com.sheridan.gcr.client.render.fx.muzzleSmoke.slow.SlowSmoke;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.slow.SlowSmokeEntry;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.slow.SlowSmokeTask;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    /** slow smoke 的 task 队列，key 与 fast smoke 区分开 */
    public static final Map<String, SlowSmokeTasks> SLOW_MUZZLE_SMOKE_TASKS = new HashMap<>();
    /** 每个枪口最多同时保留多少个 slow smoke task（双向队列上限） */
    public static final int MAX_SLOW_SMOKE_TASKS = 12;
    private static final String SLOW_SMOKE_ID_SUFFIX = ":slow_smoke";
    private static final Vector3f DISTANCE_SORTING = new Vector3f();
    private static final List<RenderEntry> UNIFIED_RENDER_QUEUE = new ArrayList<>();
    // 对象池，避免每帧 new RenderEntry
    private static final ArrayDeque<RenderEntry> ENTRY_POOL = new ArrayDeque<>();

    private static final class RenderEntry {
        float z;
        byte type; // 0 = fast smoke, 1 = flash, 2 = slow smoke
        MuzzleSmokeTask smokeTask;
        SlowSmokeTask slowSmokeTask;
        MuzzleEntry muzzleEntry;
        PoseStack.Pose bonePose;
        long startTime;

        void setSmoke(float z, MuzzleSmokeTask task) {
            this.z = z; this.type = 0; this.smokeTask = task;
        }
        void setSlowSmoke(float z, SlowSmokeTask task) {
            this.z = z; this.type = 2; this.slowSmokeTask = task;
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

    public static class SlowSmokeTasks{
        public long lastCall;
        /** 双向队列，最多 {@link #MAX_SLOW_SMOKE_TASKS} 个，新的从头部进，最老的从尾部丢 */
        public Deque<SlowSmokeTask> queue;

        public SlowSmokeTasks(long lastCall) {
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
                    Client.WEAPON_STATUS.setMuzzleFlashConfig(bonePose, entry.flashLightIntensity * (0.9f + 0.2f * Client.WEAPON_STATUS.shootRandomSeed));
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
                SlowSmokeEntry smokeEntry = entry.getSlowSmokeEntry();
                // 贴图集合既可以挂在 MuzzleEntry 上，也可以直接挂在它自己的 SlowSmokeEntry 上
                SlowSmoke slowSmoke = entry.getSlowSmoke() != null
                        ? entry.getSlowSmoke()
                        : (smokeEntry == null ? null : smokeEntry.getSlowSmoke());
                if (slowSmoke != null && smokeEntry != null) {
                    String id = context.currentRenderNode().id + entry.getName() + SLOW_SMOKE_ID_SUFFIX;
                    SlowSmokeTasks tasks = SLOW_MUZZLE_SMOKE_TASKS.get(id);
                    if (tasks == null) {
                        // 时间戳可能已经过期（GunEffectManager 会一直保留最后一次射击的时间），
                        // 这种时候不要再补一个已经淡出完的 task
                        if (!slowSmoke.isExpired(startTime, smokeEntry)) {
                            tasks = new SlowSmokeTasks(startTime);
                            // SlowSmokeTask 内部会复制射击那一刻的矩阵位置
                            tasks.queue.addFirst(slowSmoke.createTask(bonePose, startTime, smokeEntry, context.light));
                            SLOW_MUZZLE_SMOKE_TASKS.put(id, tasks);
                        }
                    } else if (tasks.lastCall != startTime) {
                        // 队列有上限，超出时先丢掉最老的那一次
                        if (tasks.queue.size() >= MAX_SLOW_SMOKE_TASKS) {
                            tasks.queue.pollLast();
                        }
                        tasks.queue.offerFirst(slowSmoke.createTask(bonePose, startTime, smokeEntry, context.light));
                        tasks.lastCall = startTime;
                    }
                }
            } else if (context.isThirdPerson()) {
                entry.getMuzzleFlash().render(bonePose, context.bufferSource, entry.getScale(), startTime, false, LightTexture.FULL_BRIGHT);
            }
        }
    }

    /**
     * 统一的半透明排序键：{@code key = -pose 平移的 z}。
     * <p>
     * 这个姿态空间里 -Z 是枪口前方（远离相机），所以 key 越大 = 离相机越远。
     * 所有特效（fast smoke / slow smoke / muzzle flash）都用同一个约定，
     * 排序时按 key 降序（由远到近）画，这是半透明的标准绘制顺序。
     */
    private static float sortKey(PoseStack.Pose pose) {
        return -pose.pose().getTranslation(DISTANCE_SORTING).z;
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
                    float z = sortKey(task.pose);
                    RenderEntry re = acquireEntry();
                    re.setSmoke(z, task);
                    UNIFIED_RENDER_QUEUE.add(re);
                }

                if (tasks.queue.isEmpty()) {
                    mapIt.remove();
                }
            }
        }

        // ── 1.5 收集 Slow Smoke ──────────────────────────────────────────
        if (!SLOW_MUZZLE_SMOKE_TASKS.isEmpty()) {
            Iterator<Map.Entry<String, SlowSmokeTasks>> slowMapIt = SLOW_MUZZLE_SMOKE_TASKS.entrySet().iterator();
            while (slowMapIt.hasNext()) {
                SlowSmokeTasks tasks = slowMapIt.next().getValue();

                Iterator<SlowSmokeTask> it = tasks.queue.iterator();
                while (it.hasNext()) {
                    SlowSmokeTask task = it.next();
                    if (task.isFinished()) {
                        it.remove();
                        continue;
                    }
                    // 用烟雾推进后的位置作为排序键，和 flash / fast smoke 一起做 z 轴半透明排序
                    RenderEntry re = acquireEntry();
                    re.setSlowSmoke(task.sortDepth(), task);
                    UNIFIED_RENDER_QUEUE.add(re);
                }

                if (tasks.queue.isEmpty()) {
                    slowMapIt.remove();
                }
            }
        }

        // ── 2. 收集 Flash ────────────────────────────────────────────────
        for (Triple<MuzzleEntry, PoseStack.Pose, Long> pair : MUZZLE_FLASH_QUEUE) {
            PoseStack.Pose bonePose = pair.getMiddle();
            float z = sortKey(bonePose);
            RenderEntry re = acquireEntry();
            re.setFlash(z, pair.getLeft(), bonePose, pair.getRight());
            UNIFIED_RENDER_QUEUE.add(re);
        }

        // ── 3. 排序 ──────────────────────────────────────────────────────
        if (UNIFIED_RENDER_QUEUE.size() > 1) {
            UNIFIED_RENDER_QUEUE.sort((a, b) -> Float.compare(b.z, a.z));
        }

        // ── 4. 渲染 ──────────────────────────────────────────────────────
        // 每画完一条就把它的批次立刻发出去：
        // MC 的 BufferSource 只在"切换到另一个渲染类型"时才会把上一批发出去，
        // 缓存/复用同一个 RenderType 时批次会被合并或延后，导致 GPU 的绘制顺序和这里的 z 排序不一致。
        // 显式 endBatch(type) 之后，绘制顺序严格等于排序顺序（且类型会从 startedBuilders 里移除，不会越积越多）。
        for (RenderEntry re : UNIFIED_RENDER_QUEUE) {
            RenderType usedType = null;
            if (re.type == 0) {
                re.smokeTask.handleRender(bufferSource);
                usedType = re.smokeTask.lastRenderType();
            } else if (re.type == 2) {
                re.slowSmokeTask.handleRender(bufferSource);
                usedType = re.slowSmokeTask.lastRenderType();
            } else {
                usedType = re.muzzleEntry.getMuzzleFlash()
                        .render(re.bonePose, bufferSource, re.muzzleEntry.getScale(),
                                re.startTime, true, LightTexture.FULL_BRIGHT);
            }
            if (usedType != null && bufferSource instanceof MultiBufferSource.BufferSource batched) {
                batched.endBatch(usedType);
            }
            // 归还对象池
            re.smokeTask = null;
            re.slowSmokeTask = null;
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
        if (Client.isUsingIrisShader) {
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
