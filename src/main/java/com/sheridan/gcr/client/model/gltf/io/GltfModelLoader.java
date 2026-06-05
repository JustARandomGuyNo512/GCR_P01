package com.sheridan.gcr.client.model.gltf.io;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.client.model.MeshModelData;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

public class GltfModelLoader {
    private static AssetManager assetManager;

    private static void init() {
        assetManager = new DesktopAssetManager();
        assetManager.registerLoader(GltfLoaderProxy.class, "gltf", "glb");
        assetManager.registerLocator("", GltfAssetsLocator.class);
    }

    /**
     * 从指定的资源位置加载模型，并返回纯粹的mesh定义，不具备渲染
     * 此方法主要用于加载包含蒙皮控制的gltf模型
     *
     * @param location 模型的资源位置
     * @return 返回模型的数据结构，如果加载失败或模型不符合要求，则返回null
     * */
    public static MeshModelData loadModel(ResourceLocation location) {
        // 确保assetManager已初始化
        if (assetManager == null) {
            init();
        }
        // 加载模型
        Spatial model = assetManager.loadModel(location.toString());

        // 获取蒙皮控制，如果没有蒙皮控制，则输出错误信息并返回null
        SkinningControl control = model.getControl(SkinningControl.class);
        if (control == null) {
            System.err.println("Skinning UnFound, please check your model");
            return null;
        }

        // 初始化模型数据结构
        MeshModelData root = null;
        Map<Integer, MeshModelData> boneMap = new HashMap<>();

        // 遍历骨架中的所有关节，构建模型的数据结构
        Armature armature = control.getArmature();
        for (Joint joint : armature.getJointList()) {
            MeshModelData bone = new MeshModelData();

            // 设置根骨骼
            if ("root".equals(joint.getName())) {
                System.out.println("found root for: " + location + " " + bone);
                root = bone;
            }

            // 将关节与模型数据结构中的骨骼对应
            boneMap.put(joint.getId(), bone);

            // 处理父子关系
            Joint parent = joint.getParent();
            if (parent != null) {
                MeshModelData parentPart = boneMap.get(parent.getId());
                if (parentPart == null) {
                    parentPart = new MeshModelData();
                    boneMap.put(parent.getId(), parentPart);
                }
                parentPart.addChild(joint.getName(), bone);
            }

            // 设置骨骼的初始姿态
            Vector3f localTranslation = joint.getLocalTranslation();
            Quaternion localRotation = joint.getLocalRotation();
            float[] angles = quaternionToEulerZYX(localRotation);
            bone.setPose(PartPose.offsetAndRotation(
                    localTranslation.x,
                    localTranslation.y,
                    localTranslation.z,
                    angles[0],
                    angles[1],
                    angles[2]));
        }

        // 如果没有找到根骨骼，则输出错误信息并返回null
        if (root == null) {
            throw new RuntimeException("'root' bone unfound, please check your model: " +  location);
        }

        // 构建旋转轴点
        Map<MeshModelData, Pair<org.joml.Matrix4f, org.joml.Matrix3f>> pivots = new HashMap<>();
        PoseStack poseStack = new PoseStack();
        buildPivots(root, pivots, poseStack);

        // 遍历模型，加载mesh数据
        model.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry geometry) {
                Mesh mesh = geometry.getMesh();

                if (mesh != null) {
                    loadMesh(mesh, boneMap, pivots);
                }
            }
        });

        // 返回模型的数据结构
        return root;
    }

    /**
     * 返回单位：度
     * */
    private static float[] quaternionToEulerZYX(Quaternion q) {
        float x = q.getX();
        float y = q.getY();
        float z = q.getZ();
        float w = q.getW();

        // 以下按 ZYX 顺序解析欧拉角
        float sinRCosP = 2 * (w * x + y * z);
        float cosRCosP = 1 - 2 * (x * x + y * y);
        float rollX = (float)Math.atan2(sinRCosP, cosRCosP);

        float sinP = 2 * (w * y - z * x);
        float pitchY;
        if (Math.abs(sinP) >= 1)
            pitchY = (float)(Math.copySign(Math.PI / 2, sinP));
        else
            pitchY = (float)Math.asin(sinP);

        float sinYCosP = 2 * (w * z + x * y);
        float cosYCosP = 1 - 2 * (y * y + z * z);
        float yawZ = (float)Math.atan2(sinYCosP, cosYCosP);

        return new float[] {
               rollX,
               pitchY,
               yawZ
        };
    }
    /**
     * 递归构建骨骼的轴点
     * 此方法用于遍历骨骼结构，并计算每个骨骼在特定姿势堆栈下的轴点位置
     * 轴点位置对于后续的骨骼动画计算至关重要
     *
     * @param bone 当前处理的骨骼对象
     * @param pivots 存储骨骼与其中心轴点映射的字典
     * @param poseStack 姿势堆栈，用于管理骨骼变换
     */
    protected static void buildPivots(MeshModelData bone, Map<MeshModelData, Pair<org.joml.Matrix4f, org.joml.Matrix3f>> pivots, PoseStack poseStack) {
        // 将当前骨骼的变换添加到姿势堆栈中
        poseStack.pushPose();
        bone.translateAndRotate(poseStack);
        // 获取当前骨骼变换后的翻译部分，用于计算轴点位置
        org.joml.Matrix4f pose = new org.joml.Matrix4f(poseStack.last().pose());
        Matrix4f invert = pose.invert();
        org.joml.Matrix3f normal = new org.joml.Matrix3f();
        invert.normal(normal); // inverse-transpose 3x3
        pivots.put(bone, Pair.of(pose, normal));
        // 遍历当前骨骼的所有子骨骼，递归调用本方法以构建它们的轴点
        for (MeshModelData child : bone.getChildren().values()) {
            buildPivots(child, pivots, poseStack);
        }
        // 从姿势堆栈中移除当前骨骼的变换，以便回溯到上一级骨骼的状态
        poseStack.popPose();
    }


    /**
     * 加载网格数据，并根据骨骼权重分配顶点到对应的骨骼模型数据中
     * 此方法主要用于处理网格中的顶点数据，包括位置、法线、纹理坐标和骨骼权重信息，
     * 并根据最大的骨骼权重决定顶点应属于的骨骼模型数据
     *
     * @param mesh 网格对象，包含顶点数据和索引数据
     * @param boneMap 骨骼模型数据映射，键为骨骼索引，值为对应的骨骼模型数据
     * @param pivotPoseMap 骨骼模型数据对应的轴点位置映射，键为骨骼模型数据，值为轴点位置
     */
    protected static void loadMesh(
            Mesh mesh,
            Map<Integer, MeshModelData> boneMap,
            Map<MeshModelData, Pair<org.joml.Matrix4f, org.joml.Matrix3f>> pivotPoseMap) {
        // 获取顶点位置、法线、纹理坐标和索引缓冲区
        FloatBuffer pos = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        FloatBuffer nor = mesh.getFloatBuffer(VertexBuffer.Type.Normal);
        FloatBuffer uv = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
        IndexBuffer indexBuffer = mesh.getIndexBuffer();
        ShortBuffer boneIndexBuffer = mesh.getShortBuffer(VertexBuffer.Type.BoneIndex);
        FloatBuffer boneWeights = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);

        int componentsPerVertex = mesh.getBuffer(VertexBuffer.Type.BoneIndex).getNumComponents();
        final Vector4f tempPos = new Vector4f();
        final org.joml.Vector3f tempNor = new org.joml.Vector3f();
        // 将索引缓冲区数据复制到数组中
        int[] indices = new int[indexBuffer.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indexBuffer.get(i);
        }


        // 遍历每个索引，处理顶点数据
        for (int i = 0; i < indices.length; i++) {
            int vertexIndex = indices[i];

            // 读取顶点位置、法线和纹理坐标
            float x = pos.get(vertexIndex * 3);
            float y = pos.get(vertexIndex * 3 + 1);
            float z = pos.get(vertexIndex * 3 + 2);

            float nx = nor.get(vertexIndex * 3);
            float ny = nor.get(vertexIndex * 3 + 1);
            float nz = nor.get(vertexIndex * 3 + 2);

            float u = uv.get(vertexIndex * 2);
            float v = uv.get(vertexIndex * 2 + 1);

            // 初始化最大权重和对应的骨骼索引
            float maxWeight = Float.MIN_VALUE;
            int fineBoneIndex = 1;
            // 遍历每个顶点的骨骼权重，找到最大权重对应的骨骼索引
            for (int j = 0; j < componentsPerVertex; j ++) {
                int boneIndex = boneIndexBuffer.get(vertexIndex * componentsPerVertex + j);
                float boneWeight = boneWeights.get(vertexIndex * componentsPerVertex + j);
                if (boneWeight > maxWeight) {
                    fineBoneIndex = boneIndex;
                    maxWeight = boneWeight;
                }
            }

            // 根据骨骼索引获取对应的骨骼模型数据和轴点位置
            MeshModelData bone = boneMap.get(fineBoneIndex);

            Pair<Matrix4f, Matrix3f> pose = pivotPoseMap.get(bone);
            if (pose != null) {
                Matrix4f inv = pose.getLeft();
                Matrix3f normalMat = pose.getRight();

                tempPos.set(x, y, z, 1.0f);
                inv.transform(tempPos);
                x = tempPos.x;
                y = tempPos.y;
                z = tempPos.z;

                tempNor.set(nx, ny, nz);
                normalMat.transform(tempNor);
                tempNor.normalize();

                nx = tempNor.x;
                ny = tempNor.y;
                nz = tempNor.z;
            }

            // 将处理后的顶点数据添加到对应的骨骼模型数据中
            int index = indexBuffer.get(i);
            bone.pushVertex(x, y, z, u, v, nx, ny, nz, index);
        }
    }

}
