package com.sheridan.gcr.modularSys.util;

import com.sheridan.gcr.modularSys.IVoxel;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CollisionChecker {
    private static final float ROT_EPSILON = 1e-4f;

    public static boolean isCollided(IVoxel voxel, Matrix4f voxelPos, Vector3f pivot, Matrix4f pivotPos) {
        // voxel 的包围盒
        AABB boundingBox = voxel.getBoundingBox();

        // pivot 在世界坐标中的位置
        Vector3f pivotWorld = new Vector3f(pivot).mulPosition(pivotPos);

        // voxel 的旋转情况
        Vector3f rot = voxelPos.getEulerAnglesXYZ(new Vector3f());
        boolean rotated = hasRotation(rot);

        if (!rotated) {
            // 无旋转时：退化为 AABB vs 点
            Vector3f trans = voxelPos.getTranslation(new Vector3f());

            if (!boundingBox.move(trans).contains(pivotWorld.x, pivotWorld.y, pivotWorld.z)) {
                return false;
            }

            List<AABB> boxes = voxel.getInnerShape();

            for (AABB box : boxes) {
                AABB moved = box.move(trans);
                if (moved.contains(pivotWorld.x, pivotWorld.y, pivotWorld.z)) {
                    return true;
                }
            }
            return false;
        }

        // 有旋转时：OBB vs 点
        OBB obb = OBB.fromAABB(boundingBox, voxelPos);
        if (!obb.intersects(pivotWorld)) {
            return false;
        }

        List<AABB> boxes = voxel.getInnerShape();
        List<OBB> obbList = toOBBs(boxes, voxelPos);

        for (OBB inner : obbList) {
            if (inner.intersects(pivotWorld)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCollided(IVoxel voxelA, Matrix4f aPos,
                                     IVoxel voxelB, Matrix4f bPos) {

        AABB boundingBoxA = voxelA.getBoundingBox();
        AABB boundingBoxB = voxelB.getBoundingBox();

        Vector3f rotA = aPos.getEulerAnglesXYZ(new Vector3f());
        Vector3f rotB = bPos.getEulerAnglesXYZ(new Vector3f());

        boolean rotateA = hasRotation(rotA);
        boolean rotateB = hasRotation(rotB);

        if (!rotateA && !rotateB) {
            if (!boundingBoxA.intersects(boundingBoxB)) {
                return false;
            }

            List<AABB> boxesA = voxelA.getInnerShape();
            List<AABB> boxesB = voxelB.getInnerShape();

            Vector3f transA = aPos.getTranslation(new Vector3f());
            Vector3f transB = bPos.getTranslation(new Vector3f());

            for (AABB boxA : boxesA) {
                AABB movedA = boxA.move(transA.x, transA.y, transA.z);
                for (AABB boxB : boxesB) {
                    AABB movedB = boxB.move(transB.x, transB.y, transB.z);
                    if (movedA.intersects(movedB)) {
                        return true;
                    }
                }
            }
            return false;
        }

        OBB boundingBoxOBBA = OBB.fromAABB(boundingBoxA, aPos);
        OBB boundingBoxOBBB = OBB.fromAABB(boundingBoxB, bPos);

        if (!boundingBoxOBBA.intersects(boundingBoxOBBB)) {
            return false;
        }

        List<AABB> boxesA = voxelA.getInnerShape();
        List<AABB> boxesB = voxelB.getInnerShape();

        List<OBB> obbListA = toOBBs(boxesA, aPos);
        List<OBB> obbListB = toOBBs(boxesB, bPos);

        for (OBB obbA : obbListA) {
            for (OBB obbB : obbListB) {
                if (obbA.intersects(obbB)) {
                    return true;
                }
            }
        }
        return false;
    }



    private static boolean hasRotation(Vector3f rot) {
        return Math.abs(rot.x) > ROT_EPSILON ||
                Math.abs(rot.y) > ROT_EPSILON ||
                Math.abs(rot.z) > ROT_EPSILON;
    }

    private static List<OBB> toOBBs(List<AABB> boxes, Matrix4f transform) {
        List<OBB> result = new ArrayList<>();
        for (AABB box : boxes) {
            result.add(OBB.fromAABB(box, transform));
        }
        return result;
    }
}
