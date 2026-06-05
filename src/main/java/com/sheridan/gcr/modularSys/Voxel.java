package com.sheridan.gcr.modularSys;

import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class Voxel implements IVoxel{
    private final List<AABB> shapes = new ArrayList<>();
    private final AABB boundingBox;

    public Voxel(List<AABB> shapes) {
        this.shapes.addAll(shapes);
        if (this.shapes.isEmpty()) {
            this.boundingBox = new AABB(0, 0, 0, 0, 0, 0);
        } else {
            AABB first = this.shapes.getFirst();
            float minX = (float) first.minX,
                    minY = (float) first.minY,
                    minZ = (float) first.minZ,
                    maxX = (float) first.maxX,
                    maxY = (float) first.maxY,
                    maxZ = (float) first.maxZ;
            for (int i = 1; i < this.shapes.size(); i++) {
                AABB aabb = this.shapes.get(i);
                minX = minX < aabb.minX ? minX : (float) aabb.minX;
                minY = minY < aabb.minY ? minY : (float) aabb.minY;
                minZ = minZ < aabb.minZ ? minZ : (float) aabb.minZ;
                maxX = maxX > aabb.maxX ? maxX : (float) aabb.maxX;
                maxY = maxY > aabb.maxY ? maxY : (float) aabb.maxY;
                maxZ = maxZ > aabb.maxZ ? maxZ : (float) aabb.maxZ;
            }
            this.boundingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    @Override
    public List<AABB> getInnerShape() {
        return shapes;
    }

    @Override
    public AABB getBoundingBox() {
        return this.boundingBox;
    }

    public IVoxel copy() {
        List<AABB> shapeList = new ArrayList<>();
        for (AABB aabb : this.shapes) {
            shapeList.add(new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ));
        }
        return new Voxel(shapeList);
    }
}
