package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.modular.IAnimationControllerModel;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ModuleRenderContext {
    public ModuleRenderNode root;
    public ItemStack itemStack;
    public int light;
    public int overlay;
    public float partialTicks;
    public ItemDisplayContext displayContext;
    public IGun gun;
    public PoseStack poseStack;
    public MultiBufferSource bufferSource;
    public Int2ObjectOpenHashMap<Object> localStorage;
    public LivingEntity entity;
    protected Map<RenderType, VertexConsumer> bufferCache;
    protected ModuleRenderNode currentRenderNode;
    protected Set<String> stateLockedBones;

    public ModuleRenderContext(LivingEntity entity, ModuleRenderNode root, ItemStack itemStack, float partialTicks, int light, int overlay,
                               ItemDisplayContext displayContext, IGun gun, PoseStack poseStack, MultiBufferSource bufferSource) {
        this.root = root;
        this.itemStack = itemStack;
        this.light = light;
        this.overlay = overlay;
        this.partialTicks = partialTicks;
        this.displayContext = displayContext;
        this.gun = gun;
        this.poseStack = poseStack;
        this.entity = entity;
        this.bufferSource = bufferSource;
    }

    public void startRender() {
        renderNodeTree(this.root);
    }

    public void setLocalStorage(int id, Object value) {
        if (localStorage == null) {
            localStorage = new Int2ObjectOpenHashMap<>();
        }
        localStorage.put(id, value);
    }

    public void addStateLockBone(String state) {
        if (stateLockedBones == null) {
            stateLockedBones = new HashSet<>();
        }
        stateLockedBones.add(state);
    }

    public void addStateLockBones(Set<String> state) {
        if (stateLockedBones == null) {
            stateLockedBones = new HashSet<>();
        }
        stateLockedBones.addAll(state);
    }

    public boolean isBoneStateLocked(String state) {
        return stateLockedBones != null && stateLockedBones.contains(state);
    }

    public void clearStateLockedBones() {
        if (stateLockedBones != null) {
            stateLockedBones.clear();
        }
    }

    /**
     * 只在明确知道缓存原始类型时使用
     * */
    public Object getLocalStorage(int id) {
        if (this.localStorage != null) {
            return this.localStorage.get(id);
        }
        return null;
    }

    public<T> T getLocalStorage(int id, Class<T> type) {
        if (this.localStorage != null) {
            Object o = this.localStorage.get(id);
            if (type.isInstance(o)) {
                return type.cast(o);
            }
        }
        return null;
    }

    public boolean removeLocalStorage(int id) {
        if (this.localStorage != null) {
            return this.localStorage.remove(id) != null;
        }
        return false;
    }

    public void clearLocalStorage() {
        if (this.localStorage != null) {
            this.localStorage.clear();
        }
    }

    public ModuleRenderNode getRoot() {
        return root;
    }
    /**
     * 直接获取buffer，与原版用法一致
     * */
    public VertexConsumer getBuffer(RenderType renderType) {
        return bufferSource.getBuffer(renderType);
    }

    /**
     * 从缓存获取或创建buffer，在renderType的canConsolidateConsecutiveGeometry()返回false时使用此方法
     * 可以降低renderType乱序调用时来的额外endBatch操作
     * */
    public VertexConsumer getOrCreateBuffer(RenderType renderType) {
        if (renderType.canConsolidateConsecutiveGeometry()) {
            return bufferSource.getBuffer(renderType);
        }
        if (this.bufferCache == null) {
            this.bufferCache = new Object2ObjectOpenHashMap<>(16);
        }
        VertexConsumer vc = this.bufferCache.get(renderType);
        if (vc != null) return vc;
        vc = bufferSource.getBuffer(renderType);
        this.bufferCache.put(renderType, vc);
        return vc;
    }

    public void clearBufferCache() {
        if (bufferCache != null) {
            this.bufferCache.clear();
        }
    }

    protected void handleAnimation() {
        if (isThirdPerson()) {
            if (root.model instanceof IAnimationControllerModel controllerModel) {
                controllerModel.applyThirdPersonAnimation(this);
            }
        }
    }
    protected void renderNodeTree(ModuleRenderNode root) {
        this.currentRenderNode = root;
        handleAnimation();
        if (root.model instanceof IStateViewerModel<?> model) {
            IStateViewer<?> viewer = model.getViewer();
            if (viewer != null) {
                viewer.applyState(currentRenderNode().model, this, currentRenderNode.getStates());
            }
            clearStateLockedBones();
        }
        root.initTranslate(poseStack);
        handleRender(root.model);
        if (root.slots != null) {
            for (Map.Entry<String, List<ModuleRenderNode>> entry : root.slots.entrySet()) {
                String key = entry.getKey();
                PoseStack.Pose slotPose = root.model.getBonePose(key);
                if (slotPose == null) {
                    continue;
                }
                Utils.overridePose(poseStack, slotPose);
                for (ModuleRenderNode node : entry.getValue()) {
                    poseStack.pushPose();
                    renderNodeTree(node);
                    poseStack.popPose();
                }
            }
        }
    }

    protected void handleThirdPersonAnimation() {

    }



    public boolean isFirstPerson() {
        return displayContext != null && displayContext.firstPerson();
    }

    public boolean isThirdPerson() {
        return displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    public ModuleRenderNode currentRenderNode() {
        return currentRenderNode;
    }

    public void setCurrentRenderNode(ModuleRenderNode currentRenderNode) {
        this.currentRenderNode = currentRenderNode;
    }


    public int getCurrentParam(String key) {
        return currentRenderNode == null ? -1 : currentRenderNode.getCustomParam(key);
    }

    protected void handleRender(IModularModel model) {
        model.updateBoneRenderStatus(this);
        model.render(this);
    }
}
