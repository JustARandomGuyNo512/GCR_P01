package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.BoneRenderStatus;
import com.sheridan.gcr.client.model.modular.IAnimationControllerModel;
import com.sheridan.gcr.client.model.modular.IArmHandlerModel;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.client.model.modular.state.ReadOnlyTag;
import com.sheridan.gcr.client.model.playerArm.BufferedPlayerArmModel;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellRenderer;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class FirstPersonRenderContext extends ModuleRenderContext implements IRenderStatusCacheContext {
    public final Map<ModuleRenderNode, Map<String, BoneRenderStatus>> tempStatusMap = new HashMap<>();
    public boolean renderMode = true;
    public boolean saveTempPose = false;
    private final List<ModuleRenderNode> stateViewers = new ArrayList<>();
    private final List<ModuleRenderNode> allRenderNodes = new ArrayList<>();
    protected IArmHandlerModel leftArm;
    protected IArmHandlerModel rightArm;

    public FirstPersonRenderContext(
            LivingEntity entity, ModuleRenderNode root, ItemStack itemStack,
            float partialTicks, int light, int overlay, IGun gun, PoseStack poseStack, MultiBufferSource bufferSource) {
        super(entity, root, itemStack, partialTicks, light, overlay, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, gun, poseStack, bufferSource);
        String leftArmHoldID = gun.getLeftArmHoldID(itemStack);
        String rightArmHoldID = gun.getRightArmHoldID(itemStack);
        root.dfsTravel(node -> {
            if (node.model instanceof IArmHandlerModel armHandlerModel) {
                if (Objects.equals(leftArmHoldID, node.id) && armHandlerModel.has(false)) {
                    this.leftArm = armHandlerModel;
                }
                if (Objects.equals(rightArmHoldID, node.id) && armHandlerModel.has(true)) {
                    this.rightArm = armHandlerModel;
                }
            }
        });
        //default
        if (leftArm == null || rightArm == null) {
            if (root.model instanceof IArmHandlerModel model) {
                if (leftArm == null && model.has(false)) {
                    this.leftArm = model;
                }
                if (rightArm == null && model.has(true)) {
                    this.rightArm = model;
                }
            }
        }
    }

    public void frameUpdate(PoseStack poseStack, int light, int overlay, float partialTicks) {
        this.poseStack = poseStack;
        this.light = light;
        this.overlay = overlay;
        this.partialTicks = partialTicks;
        this.entity = Minecraft.getInstance().player;
        CompoundTag states = this.gun.getStatesTag(itemStack);
        for (ModuleRenderNode node : stateViewers) {
            if (states.contains(node.id)) {
                ReadOnlyTag nodeStates = node.getStates();
                if (nodeStates == null) {
                    node.setStates(new ReadOnlyTag(states.getCompound(node.id)));
                } else {
                    node.getStates().setRef(states.getCompound(node.id));
                }
            }
        }
    }

    public void onContextInit() {
        this.root.dfsTravel(node -> {
            if (node.model instanceof IStateViewerModel<?>) {
                stateViewers.add(node);
            }
            allRenderNodes.add(node);
            Map<String, BoneRenderStatus> temp = new HashMap<>();
            root.model.getRootBone().deptFirstTravel(bone -> temp.put(bone.name, bone.renderStatus.copy()));
            tempStatusMap.put(node, temp);
        });
    }

    public void calcPose() {
        this.calcPose(root);
    }

    private void calcPose(ModuleRenderNode root) {
        this.currentRenderNode = root;
        root.initTranslate(poseStack);
        IStateViewer<?> viewer = null;

        if (renderMode) {//应用动画
            if (root.model instanceof IStateViewerModel<?> stateListenerModel) {
                viewer = stateListenerModel.getViewer();
            }
            if (root.model instanceof IAnimationControllerModel model) {
                model.applyFirstPersonAnimation(this);
                model.applyCustomFirstPersonAnimation(this);
            }
            if (viewer != null) {
                viewer.applyState(root.model, this, currentRenderNode.getStates());
            }
            this.clearStateLockedBones();
        }

        root.model.updateBoneRenderStatus(this);
        root.model.preFirstPersonRender(this);
        Map<String, BoneRenderStatus> temp = getRenderStatusMap().get(currentRenderNode);
        if (temp != null) {
            root.model.getRootBone().deptFirstTravel(bone -> {
                BoneRenderStatus status = temp.get(bone.name);
                if (status != null) {
                    status.copyFrom(bone.renderStatus);
                } else {
                    temp.put(bone.name, bone.renderStatus.copy());
                }
            });
        }

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
                    this.calcPose(node);
                    poseStack.popPose();
                }
            }
        }
    }

    @Override
    protected void renderNodeTree(ModuleRenderNode root) {
        if (!renderMode) {
            return;
        }
        recordArms();
        handleRender();
    }

    protected void handleBulletShellRender() {
        BulletShellRenderer.renderInFirstPerson(light);
    }

    protected boolean canRenderArm() {
        if (leftArm == null || rightArm == null) {
            return false;
        }
        if (IrisCompat.isRenderingShadowPass()) {
            return false;
        }
        return BufferedPlayerArmModel.getInstance().checkSkinTexture();
    }

    protected void handleArmRender() {
        if (canRenderArm()) {
            BufferedPlayerArmModel.getInstance().render(true);
        }
    }

    protected void recordArms() {
        if (canRenderArm()) {
            BufferedPlayerArmModel.getInstance().hideAll();
            recordArm(false, leftArm,
                    getLocalStorage(RenderConstants.ANIMATED_LEFT_ARM_MODEL),
                    getLocalStorage(RenderConstants.LEFT_ARM_LERP_CONTROL));

            recordArm(true, rightArm,
                    getLocalStorage(RenderConstants.ANIMATED_RIGHT_ARM_MODEL),
                    getLocalStorage(RenderConstants.RIGHT_ARM_LERP_CONTROL));
        }
    }

    private void recordArm(boolean isRight, IArmHandlerModel baseArm, Object animatedArmObj, Object lerpControlObj) {
        IArmHandlerModel animatedArm = (animatedArmObj instanceof IArmHandlerModel a && a != baseArm) ? a : null;
        boolean isSlim = BufferedPlayerArmModel.isPlayerModelSlim();
        if (animatedArm != null && lerpControlObj instanceof Float progress && progress > 0) {
            PoseStack.Pose from, to;
            from = baseArm.getPose(isRight, isSlim);
            to = animatedArm.getPose(isRight, isSlim);
            PoseStack.Pose pose = Utils.lerpPose(from, to, progress);
            BufferedPlayerArmModel.getInstance().record(pose, !isRight, isSlim, light);
        } else {
            BufferedPlayerArmModel.getInstance().record(baseArm.getPose(isRight, isSlim).copy(), !isRight, isSlim, light);
        }
    }

    private void handleRender() {
        for (ModuleRenderNode node : allRenderNodes) {
            Map<String, BoneRenderStatus> stringPoseMap = getRenderStatusMap().get(node);
            if (stringPoseMap == null) {
                continue;
            }
            currentRenderNode = node;
            node.model.copyRenderStatus(stringPoseMap);
            node.model.render(this);
        }
        handleArmRender();
        handleBulletShellRender();
    }

    @Override
    public Map<ModuleRenderNode, Map<String, BoneRenderStatus>> getRenderStatusMap() {
        return tempStatusMap;
    }

    public ReadOnlyTag getNodeStates(String bulletShellHandlerNodeID) {
        CompoundTag nodeStatesTag = gun.getNodeStatesTag(itemStack, bulletShellHandlerNodeID);
        return ReadOnlyTag.of(nodeStatesTag);
    }
}