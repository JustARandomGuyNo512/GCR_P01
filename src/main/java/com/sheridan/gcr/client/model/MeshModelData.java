package com.sheridan.gcr.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshModelData {
    public static final String ROOT = "root";
    protected final List<Vertex> vertices;
    protected final Map<String, MeshModelData> children;
    protected PartPose pose = PartPose.ZERO;
    protected Vector3f scale = new Vector3f(1,1,1);
    protected MeshModelData parent;
    protected String name;

    public MeshModelData() {
        this.children = new HashMap<>();
        this.vertices = new ArrayList<>();
        name = ROOT;
    }

    public void addChild(String name, MeshModelData bone) {
        this.children.put(name, bone);
        bone.parent = this;
        bone.name = name;
    }

    public void setPose(PartPose partPose) {
        this.pose = partPose;
    }

    public void print() {
        this.print(16);
    }

    public void print(float size) {
        System.out.println("{name: " + name + " parent: " + (parent == null ? "null" : parent.name));
        System.out.println("x: " + (pose.x * size) + ", y: " + (pose.y * size) + ", z: " + (pose.z * size));
        System.out.println("xRot: " + (Math.toDegrees(pose.xRot)) + ", yRot: " + (Math.toDegrees(pose.yRot)) + ", zRot: " + (Math.toDegrees(pose.zRot)));
        System.out.println("xScale: " + scale.x + ", yScale: " + scale.y + ", zScale: " + scale.z);
        System.out.println("vertex count: " + vertices.size() + "}\n");
        for (Map.Entry<String, MeshModelData> partEntry : children.entrySet()) {
            partEntry.getValue().print(size);
        }
    }

    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(pose.x , pose.y, pose.z);
        if (pose.xRot != 0.0F || pose.yRot != 0.0F || pose.zRot != 0.0F) {
            poseStack.mulPose((new Quaternionf()).rotationZYX(pose.zRot, pose.yRot, pose.xRot));
        }
        if (scale.x != 1.0F || scale.y != 1.0F || scale.z != 1.0F) {
            poseStack.scale(scale.x, scale.y, scale.z);
        }
    }

    public Map<String, MeshModelData> getChildren() {
        return children;
    }

    public MeshModelData getParent() {
        return parent;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public PartPose getPose() {
        return pose;
    }

    public Vector3f getScale() {
        return scale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (parent != null) {
            if (parent.getChildren().containsKey(name)) {
                return;
            }
            parent.getChildren().remove(this.name);
            parent.getChildren().put(name, this);
        }
        this.name = name;
    }

    public void depthFirstTraversal(Visitor visitor) {
        visitor.visit(this);
        for (Map.Entry<String, MeshModelData> partEntry : children.entrySet()) {
            partEntry.getValue().depthFirstTraversal(visitor);
        }
    }

    public void pushVertex(float v, float v1, float v2, float u, float v3, float nx, float ny, float nz, int index) {
        vertices.add(new Vertex(v, v1, v2, u, v3, nx, ny, nz, index));
    }

    public interface Visitor {
        void visit(MeshModelData modelData);
    }
}
