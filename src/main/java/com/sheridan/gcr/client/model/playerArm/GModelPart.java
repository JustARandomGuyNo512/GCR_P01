package com.sheridan.gcr.client.model.playerArm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GModelPart {
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    public boolean visible = true;
    public boolean skipDraw;
    public List<GCube> cubes;
    public Map<String, GModelPart> children;
    public PartPose initialPose;

    public GModelPart(List<GCube> cubes, Map<String, GModelPart> children, PartPose initialPose) {
        this.initialPose = initialPose;
        this.cubes = cubes;
        this.children = children;
    }

    public void loadPose(PartPose partPose) {
        this.x = partPose.x;
        this.y = partPose.y;
        this.z = partPose.z;
        this.xRot = partPose.xRot;
        this.yRot = partPose.yRot;
        this.zRot = partPose.zRot;
        this.xScale = 1.0F;
        this.yScale = 1.0F;
        this.zScale = 1.0F;
    }

    public void copyFrom(ModelPart modelPart) {
        this.xScale = modelPart.xScale;
        this.yScale = modelPart.yScale;
        this.zScale = modelPart.zScale;
        this.xRot = modelPart.xRot;
        this.yRot = modelPart.yRot;
        this.zRot = modelPart.zRot;
        this.x = modelPart.x;
        this.y = modelPart.y;
        this.z = modelPart.z;
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        this.render(poseStack, buffer, packedLight, packedOverlay, -1);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (this.visible && (!this.cubes.isEmpty() || !this.children.isEmpty())) {
            poseStack.pushPose();
            this.translateAndRotate(poseStack);
            if (!this.skipDraw) {
                this.compile(poseStack.last(), buffer, packedLight, packedOverlay, color);
            }

            for(GModelPart modelPart : this.children.values()) {
                modelPart.render(poseStack, buffer, packedLight, packedOverlay, color);
            }

            poseStack.popPose();
        }

    }

    private void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        for(GCube cube : this.cubes) {
            cube.compile(pose, buffer, packedLight, packedOverlay, color);
        }

    }

    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            poseStack.mulPose((new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    public GModelPart getChild(String name) {
        GModelPart modelPart = this.children.get(name);
        if (modelPart == null) {
            throw new NoSuchElementException("Can't find part " + name);
        } else {
            return modelPart;
        }
    }
}
