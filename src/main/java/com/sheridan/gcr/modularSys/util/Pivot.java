package com.sheridan.gcr.modularSys.util;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pivot {
    public float x, y, z;
    public float rx, ry, rz;
    public Pivot parent;
    public String name;
    public Map<String, Pivot> children;

    public Pivot(float x, float y, float z, float rx, float ry, float rz, String name, Pivot parent) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.name = name;
        this.parent = parent;
        this.children = new HashMap<>();
    }

    public String toString() {
        return "Pivot{" + "\n" +
                "x=" + x + "\n" +
                ", y=" + y + "\n" +
                ", z=" + z + "\n" +
                ", rx=" + rx + "\n" +
                ", ry=" + ry + "\n" +
                ", rz=" + rz + "\n" +
                ", name='" + name + '\'' + "\n" +
                ", parent='" + (parent == null ? "null" : parent.name) + "'" +  "\n" +
                '}';
    }

    public Pivot(Vector3f pos, Vector3f rot, String name, Pivot parent) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.rx = rot.x;
        this.ry = rot.y;
        this.rz = rot.z;
        this.name = name;
        this.parent = parent;
        this.children = new HashMap<>();
    }

    public void addChild(String slot, Pivot child) {
        this.children.put(slot, child);
    }

    public Pivot getChild(String slot) {
        return this.children.get(slot);
    }

    public List<Pivot> getPath() {
        List<Pivot> path = new ArrayList<>();
        Pivot pivot = this;
        while (pivot != null) {
            path.add(pivot);
            pivot = pivot.parent;
        }
        return path;
    }

    public Map<String, Pivot> getChildren() {
        return children;
    }

    public Matrix4f handleTransform(Matrix4f matrix4f) {
        if (parent == null) {
            return matrix4f.translate(x, y, z).rotateXYZ(rx, ry, rz);
        } else {
            List<Pivot> path = getPath();
            for (Pivot pivot : path) {
                matrix4f.translate(pivot.x, pivot.y, pivot.z).rotateXYZ(pivot.rx, pivot.ry, pivot.rz);
            }
            return matrix4f;
        }
    }
}
