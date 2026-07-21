package com.sheridan.gcr.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.animation.IAnimated;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class Bone implements IAnimated {
    public boolean visible;
    public Bone parent;
    public final String name;
    public final int index;
    public PartPose initialPose;
    public float x, y, z;
    public float xRot, yRot, zRot;
    public float xScale, yScale, zScale;
    public final Map<String, Bone> children = new Object2ObjectArrayMap<>();
    public BoneRenderStatus renderStatus;
    public final int vertexCount;
    public Object model;


    public Bone(int index, String name, int vertexCount, @Nullable Object model) {
        this.index = index;
        this.name = name;
        this.vertexCount = vertexCount;
        this.visible = true;
        this.model = model;
    }

    public void deptFirstTravel(Bone.Visitor visitor) {
        visitor.visit(this);
        for (Map.Entry<String, Bone> childEntry : children.entrySet()) {
            childEntry.getValue().deptFirstTravel(visitor);
        }
    }

    public interface Visitor {
        void visit(Bone bone);
    }

    public void addChild(String name, Bone bone) {
        this.children.put(name, bone);
        bone.parent = this;
    }

    public void loadPose(MeshModelData meshModelData) {
        initialPose = meshModelData.getPose();
        resetPose();
    }

    public void updateRenderStatus(PoseStack poseStack, int light) {
        renderStatus.visible = vertexCount > 0 && (xScale > 0 || yScale > 0 || zScale > 0);
        renderStatus.pose = poseStack.last().copy();
        renderStatus.lightmapUV = light;
    }

    public boolean renderable() {
        return renderStatus != null && renderStatus.vertexCount > 0;
    }

    public Bone getBone(String name) {
        return children.get(name);
    }

    public Bone getBoneOrThrow(String name) {
        Bone bone = children.get(name);
        if (bone == null) {
            throw new RuntimeException("Bone " + name + " not found");
        }
        return bone;
    }

    @Override
    public void offsetPos(Vector3f vector3f) {
        this.x += vector3f.x();
        this.y += vector3f.y();
        this.z += vector3f.z();
    }

    @Override
    public void offsetRotation(Vector3f vector3f) {
        this.xRot += vector3f.x();
        this.yRot += vector3f.y();
        this.zRot += vector3f.z();
    }

    @Override
    public void offsetScale(Vector3f vector3f) {
        this.xScale += vector3f.x();
        this.yScale += vector3f.y();
        this.zScale += vector3f.z();
    }

    public Bone findBone(String name) {
        if (this.name.equals(name)) {
            return this;
        }
        for (Bone child : children.values()) {
            Bone found = child.findBone(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public Optional<IAnimated> findByName(String pName) {
        Bone bone = findBone(pName);
        return bone == null ? Optional.empty() : Optional.of(bone);
    }


    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(this.x, this.y, this.z);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            poseStack.mulPose((new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
        }
        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    public void resetPoseAll() {
        resetPose();
        for (Bone child : children.values()) {
            child.resetPoseAll();
        }
    }

    public void resetPose() {
        x = initialPose.x;
        y = initialPose.y;
        z = initialPose.z;
        xRot = initialPose.xRot;
        yRot = initialPose.yRot;
        zRot = initialPose.zRot;
        xScale = 1.0F;
        yScale = 1.0F;
        zScale = 1.0F;
    }
}