package com.sheridan.gcr.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoneRenderStatus {
    public final int boneIndex;
    public PoseStack.Pose pose;
    public int lightmapUV;
    public final int vertexStart;
    public final int vertexEnd;
    public final int vertexCount;
    public boolean visible = false;


    public BoneRenderStatus(int boneIndex, int vertexStart, int vertexEnd) {
        this.boneIndex = boneIndex;
        this.vertexStart = vertexStart;
        this.vertexEnd = vertexEnd;
        this.vertexCount = vertexEnd - vertexStart;
        PoseStack poseStack = new PoseStack();
        this.pose = poseStack.last();
    }

    public BoneRenderStatus copy() {
        BoneRenderStatus status = new BoneRenderStatus(this.boneIndex, this.vertexStart, this.vertexEnd);
        status.copyFrom(this);
        return status;
    }

    public void copyFrom(BoneRenderStatus boneRenderStatus) {
        this.pose.pose().set(boneRenderStatus.pose.pose());
        this.pose.normal().set(boneRenderStatus.pose.normal());
        this.lightmapUV = boneRenderStatus.lightmapUV;
        this.visible = boneRenderStatus.visible;
    }
}
