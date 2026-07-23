package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.render.DefaultGunRenderer;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ArmHandlerModel<T extends IStateView> extends AnimatedModel<T> implements IArmHandlerModel {
    protected boolean hasLeft;
    protected boolean hasRight;
    protected Bone slimLeft;
    protected Bone maleLeft;

    protected Bone slimRight;
    protected Bone maleRight;

    protected Bone slimLeftPivot;
    protected Bone maleLeftPivot;

    protected Bone slimRightPivot;
    protected Bone maleRightPivot;

    protected Bone innerLeft;
    protected Bone innerRight;

    protected Bone leftArm;
    protected Bone rightArm;

    @Override
    public void updateBoneRenderStatus(Bone root, PoseStack poseStack, int light) {
        if (root == leftArm) {
            poseStack.pushPose();
            PoseStack testStack = Client.getGunRenderer().getEnvDisturbance();
            Matrix4f pose = testStack.last().pose();
            Quaternionf quaternionf = pose.getNormalizedRotation(new Quaternionf());
            quaternionf.conjugate();
            //抵消60%的旋转震动，先暂时这么干吧
            Quaternionf half = new Quaternionf().identity().slerp(quaternionf, 0.6f);
            poseStack.mulPose(half);
        }
        super.updateBoneRenderStatus(root, poseStack, light);
        if (root == leftArm) {
            poseStack.popPose();
        }
    }

    public ArmHandlerModel(MeshModelData root, IStateViewer<T> viewer, ResourceLocation name) {
        super(root, name, viewer);
        postProcess();
    }

    protected void postProcess() {
        for (Bone bone : this.flatBoneMap.values()) {
            if ("LEFT_ARM".equals(bone.name)) {
                if (matchArmStructure(bone, false)) {
                    leftArm = bone;
                    innerLeft = bone.getBoneOrThrow("INNER_L");
                    maleLeft = innerLeft.getBoneOrThrow("L_MALE");
                    slimLeft = innerLeft.getBoneOrThrow("L_SLIM");
                    maleLeftPivot = maleLeft.getBoneOrThrow("L_MALE_PIVOT");
                    slimLeftPivot = slimLeft.getBoneOrThrow("L_SLIM_PIVOT");
                    this.hasLeft = true;
                }
            }
            if ("RIGHT_ARM".equals(bone.name)) {
                if (matchArmStructure(bone, true)) {
                    rightArm = bone;
                    innerRight = bone.getBoneOrThrow("INNER_R");
                    maleRight = innerRight.getBoneOrThrow("R_MALE");
                    slimRight = innerRight.getBoneOrThrow("R_SLIM");
                    maleRightPivot = maleRight.getBoneOrThrow("R_MALE_PIVOT");
                    slimRightPivot = slimRight.getBoneOrThrow("R_SLIM_PIVOT");
                    this.hasRight = true;
                }
            }
        }
    }


    protected boolean matchArmStructure(Bone bone, boolean rightArm) {
        String innerLayer = rightArm ? "INNER_R" : "INNER_L";
        Bone inner = bone.getBone(innerLayer);
        if (inner != null) {
            String slim = rightArm ? "R_SLIM" : "L_SLIM";
            String male = rightArm ? "R_MALE" : "L_MALE";
            Bone slimArm = inner.getBone(slim);
            Bone maleArm = inner.getBone(male);
            return slimArm != null && maleArm != null;
        }
        return false;
    }

    @Override
    public PoseStack.Pose getPose(boolean rightArm, boolean slim) {
        if (rightArm) {
            return slim ? slimRight.renderStatus.pose : maleRight.renderStatus.pose;
        } else {
            return slim ? slimLeft.renderStatus.pose : maleLeft.renderStatus.pose;
        }
    }

    @Override
    public Bone getBone(boolean rightArm, boolean slim) {
        if (rightArm) {
            return slim ? slimRight : maleRight;
        } else {
            return slim ? slimLeft : maleLeft;
        }
    }


    @Override
    public boolean has(boolean rightArm) {
        return rightArm ? this.hasRight : this.hasLeft;
    }
}
