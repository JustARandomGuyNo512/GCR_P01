package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
import com.sheridan.gcr.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer implements IMuzzleFlashRenderer{
    public static final int DEFERRED_RENDER_TASK = 10000;
    private final Map<String, MuzzleEntry> entryMap = new HashMap<>();
    private final List<MuzzleEntry> entries = new ArrayList<>();
    protected boolean firstPersonRecorded = false;
    private static List<Triple<MuzzleEntry, PoseStack.Pose, Long>> renderQueue = new ArrayList<>();


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
                renderQueue.add(Triple.of(entry, bonePose, startTime));
            } else if (context.isThirdPerson()) {
                entry.getMuzzleFlash().render(bonePose, context.bufferSource, entry.getScale(), startTime, false, LightTexture.FULL_BRIGHT);
            }
        }
    }


    public static void renderAllFirstPerson(MultiBufferSource.BufferSource bufferSource) {
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
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {

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
