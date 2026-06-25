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
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer implements IMuzzleFlashRenderer{
    public static final int STENCIL_DEFERRED_RENDER_TASK = 10000;
    public static final int VANILLA_DEFERRED_RENDER_TASK = 10001;
    private final Map<String, MuzzleEntry> entryMap = new HashMap<>();
    private final List<MuzzleEntry> entries = new ArrayList<>();


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
    public void render(ModuleRenderContext context, IMuzzleFlashRendererModel model, GunEffect effectListener, String effectModuleId) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        if (context.entity == null) {
            return;
        }
        boolean firstPerson = context.isFirstPerson();
        renderEffect(context, model, effectModuleId, effectListener, firstPerson);
    }

    protected void renderEffect(ModuleRenderContext context, IMuzzleFlashRendererModel model, String effectModuleId,  GunEffect effectListener, boolean firstPerson) {
        for (MuzzleEntry entry : entries) {
            if (!entry.enabled) {
                continue;
            }
            String bindSlotName = entry.getBindSlotName();
            ModuleRenderNode node = context.currentRenderNode();
            if (!node.hasChild(bindSlotName)) {
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
                if (Client.isIrisShaderInUse && firstPerson) {
                    if (entry.getMuzzleFlash().shouldRender(startTime, true)) {
                        if (context.removeLocalStorage(ScopeModel.SCOPE_VIEW_RENDERING)) {
                            context.setLocalStorage(STENCIL_DEFERRED_RENDER_TASK,
                                    (Runnable) () -> stencilDeferredRender(entry, bonePose, startTime));
                        } else {
                            final Matrix4f modelViewMat = Client.getGunRenderer().firstPersonModelViewMat();
                            Stage.LOW.addTask(
                                    new Task((RenderLevelStageEvent event) -> deferredRender(modelViewMat, entry, bonePose, startTime)));
                        }
                        Client.WEAPON_STATUS.setMuzzleFlashPos(bonePose);
                    }
                } else {
                    if (firstPerson) {
                        Client.WEAPON_STATUS.setMuzzleFlashPos(bonePose);
                        PoseStack.Pose copy = bonePose.copy();

                        context.setLocalStorage(VANILLA_DEFERRED_RENDER_TASK, (Runnable) () -> entry.getMuzzleFlash().render(
                                copy,
                                context.bufferSource,
                                entry.getScale(),
                                startTime,
                                true,
                                LightTexture.FULL_BRIGHT));
                    } else {
                        entry.getMuzzleFlash().render(
                                bonePose,
                                context.bufferSource,
                                entry.getScale(),
                                startTime,
                                false,
                                LightTexture.FULL_BRIGHT);
                    }

                }
            }
        }
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        Object localStorage = context.getLocalStorage(VANILLA_DEFERRED_RENDER_TASK);
        if (localStorage instanceof Runnable runnable) {
            runnable.run();
        }
    }

    protected void stencilDeferredRender(MuzzleEntry entry, PoseStack.Pose bonePose, long startTime) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        entry.getMuzzleFlash().render(
                bonePose,
                bufferSource,
                entry.getScale(),
                startTime,
                true,
                LightTexture.FULL_BRIGHT);
        bufferSource.endBatch();
    }

    protected void deferredRender(Matrix4f modelViewMat, MuzzleEntry entry, PoseStack.Pose bonePose, long startTime) {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(Client.FIRST_PERSON_PROJECTION_MAT, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewMatrix().set(modelViewMat);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.enableDepthTest();
        entry.getMuzzleFlash().render(
                bonePose,
                bufferSource,
                entry.getScale(),
                startTime,
                true,
                LightTexture.FULL_BRIGHT);
        bufferSource.endBatch();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

}
