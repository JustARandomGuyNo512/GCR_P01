package com.sheridan.gcr.client.model.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.Bone;
import com.sheridan.gcr.client.model.MeshModelData;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.modularSys.modules.views.IStateView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmHandlerModel<T extends IStateView> extends AnimatedModel<T> implements IArmHandlerModel {
    protected boolean hasLeft;
    protected boolean hasRight;
    protected Bone slimLeft;
    protected Bone maleLeft;

    protected Bone slimRight;
    protected Bone maleRight;

    protected Bone leftArm;
    protected Bone rightArm;

    public ArmHandlerModel(MeshModelData root, IStateViewer<T> viewer, ResourceLocation name) {
        super(root, name, viewer);
        postProcess();
    }

    protected void postProcess() {
        for (Bone bone : this.flatBoneMap.values()) {
            if ("LEFT_ARM".equals(bone.name)) {
                if (matchArmStructure(bone, false)) {
                    leftArm = bone;
                    maleLeft = bone.getBone("INNER_L").getBone("L_MALE");
                    slimLeft = bone.getBone("INNER_L").getBone("L_SLIM");
                    this.hasLeft = true;
                }
            }
            if ("RIGHT_ARM".equals(bone.name)) {
                if (matchArmStructure(bone, true)) {
                    rightArm = bone;
                    maleRight = bone.getBone("INNER_R").getBone("R_MALE");
                    slimRight = bone.getBone("INNER_R").getBone("R_SLIM");
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
