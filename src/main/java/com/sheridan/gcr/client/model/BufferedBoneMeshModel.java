package com.sheridan.gcr.client.model;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.Shaders;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.mixin.VertexBufferAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL15.*;

@OnlyIn(Dist.CLIENT)
public class BufferedBoneMeshModel {
    protected CompatType compatType = CompatType.VANILLA;
    public static final int COMPILE_LIGHT = LightTexture.pack(15,15);
    //mat4 * 2
    private static final int UBO_ELEMENT_SIZE = 16 * 2;
    // UBO 在 GLSL 中的绑定点 (layout (..., binding = 0) ...)
    private static final int UBO_BINDING_POINT = 0;
    // 骨骼数量 (必须与 GLSL 中的 128 匹配)
    private static final int MAX_BONES = 128;
    /**
     * 原始数据
     * */
    protected float[] positions;
    protected float[] normals;
    protected float[] uvs;
    //bone1 index start -> bone1 index end, bone2 index start -> bone2 index end
    protected int[] boneIndices;
    /**
     * 骨骼数据
     * */
    public Bone rootBone;
    protected List<BoneRenderStatus> renderableBoneStatusList;
    protected Map<Integer, Bone> IndexToBone;
    protected Object2ObjectOpenHashMap<String, Bone> flatBoneMap;
    /**
     * 渲染工具
     * */
    protected FloatBuffer boneStatusUboBuffer;

    // UBO 的 OpenGL ID
    private int uboId = -1;
    private int irisOverride = -1;
    private int lastKnownProgramId = -1; // 用来检测 Shader 是否变化


    public RenderType renderType;
    protected VertexBuffer rawDataVertexBuffer;

    protected int boneCount = 0;
    protected int vertexCount = 0;
    protected int renderingVertexCount = 0;

    protected ResourceLocation debugName = GCR.RL("");

    public BufferedBoneMeshModel(MeshModelData root, ResourceLocation debugName) {
        this.readFromMeshData(root);
        this.debugName = debugName;
    }

    protected void readFromMeshData(MeshModelData root) {
        Map<String, Bone> boneMap = new HashMap<>();
        Map<Bone, List<Vertex>> vertexMap = new HashMap<>();
        initializeBoneMaps();
        processBoneHierarchy(root, boneMap, vertexMap);
        initializeVertexArrays();
        processVertexData(vertexMap);
    }

    private void initializeBoneMaps() {
        this.flatBoneMap = new Object2ObjectOpenHashMap<>();
        this.IndexToBone = new Object2ObjectOpenHashMap<>();
        this.renderableBoneStatusList = new ArrayList<>();
        this.boneCount = 0;
        this.vertexCount = 0;
    }

    protected void processBoneHierarchy(MeshModelData root, Map<String, Bone> boneMap, Map<Bone, List<Vertex>> vertexMap) {
        root.depthFirstTraversal((modelData) -> {
            boolean isRoot = MeshModelData.ROOT.equals(modelData.getName());
            List<Vertex> vertices = modelData.getVertices();
            Bone bone = createBone(modelData, vertices);
            boneMap.put(modelData.getName(), bone);
            updateBoneMaps(bone);
            setupBoneParent(modelData, bone, boneMap);
            vertexMap.put(bone, vertices);
            this.vertexCount += vertices.size();
            if (isRoot) {
                this.rootBone = bone;
            }
        });
    }

    protected Bone createBone(MeshModelData modelData, List<Vertex> vertices) {
        Bone bone = new Bone(this.boneCount, modelData.getName(), vertices.size(), this);
        bone.loadPose(modelData);
        this.boneCount++;
        return bone;
    }

    protected void updateBoneMaps(Bone bone) {
        this.flatBoneMap.put(bone.name, bone);
        this.IndexToBone.put(bone.index, bone);
    }

    protected void setupBoneParent(MeshModelData modelData, Bone bone, Map<String, Bone> boneMap) {
        MeshModelData parent = modelData.getParent();
        if (parent != null) {
            Bone parentBone = boneMap.get(parent.getName());
            parentBone.addChild(bone.name, bone);
        }
    }

    private void initializeVertexArrays() {
        this.positions = new float[this.vertexCount * 3];
        this.normals = new float[this.vertexCount * 3];
        this.uvs = new float[this.vertexCount * 2];
        this.boneIndices = new int[this.boneCount * 2];
    }

    private void processVertexData(Map<Bone, List<Vertex>> vertexMap) {
        List<Map.Entry<Bone, List<Vertex>>> entries = new ArrayList<>(vertexMap.entrySet());
        entries.sort(Comparator.comparingInt(o -> o.getKey().index));
        int vertexTail = 0;
        for (Map.Entry<Bone, List<Vertex>> entry : entries) {
            vertexTail = populateVertexArrays(entry, vertexTail);
            BoneRenderStatus boneRenderStatus = createBoneRenderStatus(entry.getKey(), vertexTail);
            if (boneRenderStatus.vertexCount > 0) {
                this.renderableBoneStatusList.add(boneRenderStatus);
            }
            entry.getKey().renderStatus = boneRenderStatus;
        }
    }

    private int populateVertexArrays(Map.Entry<Bone, List<Vertex>> entry, int vertexTail) {
        List<Vertex> vertices = entry.getValue();
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            int baseIndex = (vertexTail + i) * 3;
            this.positions[baseIndex] = vertex.x;
            this.positions[baseIndex + 1] = vertex.y;
            this.positions[baseIndex + 2] = vertex.z;
            this.normals[baseIndex] = vertex.normalX;
            this.normals[baseIndex + 1] = vertex.normalY;
            this.normals[baseIndex + 2] = vertex.normalZ;
            this.uvs[(vertexTail + i) * 2] = vertex.u;
            this.uvs[(vertexTail + i) * 2 + 1] = vertex.v;
        }
        this.boneIndices[entry.getKey().index] = vertexTail;
        vertexTail += vertices.size();
        this.boneIndices[entry.getKey().index + 1] = vertexTail;
        return vertexTail;
    }

    private BoneRenderStatus createBoneRenderStatus(Bone bone, int vertexTail) {
        return new BoneRenderStatus(
                bone.index,
                this.boneIndices[bone.index],
                this.boneIndices[bone.index + 1]);
    }

    public Bone getBone(String name) {
        return flatBoneMap.get(name);
    }

    public boolean hasBone(String name) {
        return flatBoneMap.containsKey(name);
    }

    public PoseStack.Pose getBoneRenderPose(String name) {
        Bone bone = getBone(name);
        return bone == null ? null : bone.renderStatus.pose;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void compile(RenderType type) {
        if (rawDataVertexBuffer != null) {
            return;
        }
        renderType = type;
        rawDataVertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder rawBuilderBuffer = new ByteBufferBuilder(1024 * 256);
        BufferBuilder rawBuilder = new BufferBuilder(rawBuilderBuffer, type.mode, type.format);
        PoseStack poseStack = new PoseStack();
        poseStack.setIdentity();
        compileVertexToBuffer(rawBuilder, poseStack.last());
        checkOrCreateUBO();
        MeshData rawData = rawBuilder.build();
        if (rawData != null) {
            if (type.sortOnUpload()) {
                rawData.sortQuads(rawBuilderBuffer, RenderSystem.getVertexSorting());
            }
            rawDataVertexBuffer.bind();
            rawDataVertexBuffer.upload(rawData);
            VertexBuffer.unbind();
        }
        rawBuilderBuffer.discard();
        rawBuilderBuffer.close();
        renderingVertexCount = 0;
        if (boneStatusUboBuffer == null) {
            boneStatusUboBuffer = BufferUtils.createFloatBuffer(UBO_ELEMENT_SIZE * renderableBoneStatusList.size());
        }
    }

    public void setRenderType(RenderType type, boolean recompile) {
        this.renderType = type;
        if (recompile) {
            compile(type);
        }
    }

    @Nullable
    public RenderType getRenderType() {
        return renderType;
    }

    private void checkOrCreateUBO() {
        if (uboId != -1) {
            return;
        }
        uboId = glGenBuffers();
        int bufferSizeInFloats = MAX_BONES * UBO_ELEMENT_SIZE;
        long bufferSizeInBytes = (long)bufferSizeInFloats * Float.BYTES;
        glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        glBufferData(GL31.GL_UNIFORM_BUFFER, bufferSizeInBytes, GL_DYNAMIC_DRAW);
        glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }


    protected boolean updateCompatType() {
        CompatType prevType = Client.isUsingIrisShader ? CompatType.IRIS : CompatType.VANILLA;
        if (compatType != prevType) {
            compatType = prevType;
            release();
            compile(renderType);
            System.out.println("Recompile for compat type: " + compatType + " vertex count: " + vertexCount);
            return false;
        }
        return true;
    }

    protected void compileVertexToBuffer(VertexConsumer rawData, PoseStack.Pose pose) {
        int boneIndex = 0;
        for (BoneRenderStatus status : renderableBoneStatusList) {
            Bone bone = IndexToBone.get(status.boneIndex);
            int vertexStart = boneIndices[bone.index];
            int vertexEnd = boneIndices[bone.index + 1];
            renderingVertexCount += bone.vertexCount;
            for (int i = vertexStart; i < vertexEnd; i++) {
                int posIndex = i * 3;
                rawData.addVertex(pose, positions[posIndex], positions[posIndex + 1], positions[posIndex + 2])
                        .setColor(1f, 1f, 1f, 1f)
                        .setUv(uvs[i * 2], uvs[i * 2 + 1])
                        .setUv1(boneIndex, 0)
                        .setLight(COMPILE_LIGHT)
                        .setNormal(pose, normals[posIndex], normals[posIndex + 1], normals[posIndex + 2]);
            }
            boneIndex++;
        }
    }

    public boolean isCompiled() {
        return rawDataVertexBuffer != null;
    }

    public int getRenderableBoneCount() {
        return renderableBoneStatusList.size();
    }
    public void release() {
        if (RenderSystem.isOnRenderThread()) {
            _release();
        } else {
            RenderSystem.recordRenderCall(this::_release);
        }
    }
    protected void _release() {
        if (rawDataVertexBuffer != null) {
            rawDataVertexBuffer.close();
            rawDataVertexBuffer = null;
        }
        if (boneStatusUboBuffer != null) {
            boneStatusUboBuffer = null;
        }
        if (RenderSystem.isOnRenderThread()) {
            if (uboId != -1) {
                GL15.glDeleteBuffers(uboId);
                uboId = -1;
            }
        } else {
            throw new IllegalStateException("Cannot release resources outside of the render thread!");
        }

        irisOverride = -1;
        lastKnownProgramId = -1;
    }

    public void render(boolean isFirstPerson, float partialTicks) {
        if (prepared()) {
            if (renderingVertexCount == 0) {
                return;
            }
            ShaderInstance shader = prepareShaderAndBuffer();
            if (shader == null) {
                return;
            }
            if (!checkShaderUbo(shader.getId())) {
                return;
            }
            shader.apply();
            prepareUbo();
            uploadUbo();
            callRender(shader, isFirstPerson, partialTicks);
            unUseShaderAndBuffer(shader);
        }
    }

    protected void callRender(ShaderInstance shader, boolean isFirstPerson, float partialTicks) {
        if (compatType == CompatType.IRIS) {
            renderInIris(shader, isFirstPerson, IrisCompat.isRenderingShadowPass(), partialTicks);
        } else if (compatType == CompatType.VANILLA) {
            renderInVanilla(shader, isFirstPerson, partialTicks);
        }
    }

    protected void unUseShaderAndBuffer(ShaderInstance shader) {
        shader.clear();
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }

    protected ShaderInstance prepareShaderAndBuffer() {
        ShaderInstance shader = getShader();
        if (shader == null) {
            return null;
        }
        renderType.setupRenderState();
        VertexBufferAccessor accessor = (VertexBufferAccessor) rawDataVertexBuffer;
        rawDataVertexBuffer.bind();
        shader.setDefaultUniforms(
                accessor.getMode(),
                RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix(),
                Minecraft.getInstance().getWindow());
        boolean renderingShadowPass = IrisCompat.isRenderingShadowPass();
        if (renderingShadowPass) {
            if (shader.PROJECTION_MATRIX != null) {
                shader.PROJECTION_MATRIX.set(IrisCompat.getShadowProjectionMat());
            }
        }
        return shader;
    }

    protected boolean prepared() {
        return rawDataVertexBuffer != null && renderType != null && updateCompatType();
    }

    public static ShaderInstance getShader() {
        return Client.isUsingIrisShader ?
                GameRenderer.getRendertypeEntityCutoutShader() :
                Shaders.getEntityCutOutUBO();
    }

    protected void renderInIris(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass, float partialTicks) {
        irisOverride = GL20.glGetUniformLocation(shader.getId(), "gcrDoTransformOverride");
        if (irisOverride == -1) {
            return;
        }
        GL20.glUniform1i(irisOverride, 1);
        renderInner(shader, isFirstPerson, isShadowPass, partialTicks);
        GL20.glUniform1i(irisOverride, 0);
    }

    protected void renderInVanilla(ShaderInstance shader, boolean isFirstPerson, float partialTicks) {
        renderInner(shader, isFirstPerson, false, partialTicks);
    }

    protected void renderInner(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass, float partialTicks) {
        afterUniformLoaded(shader, isFirstPerson, isShadowPass, partialTicks);
        draw(vertexCount, 0);
    }

    public void prepareUbo() {
        for (BoneRenderStatus status : renderableBoneStatusList) {
            loadMat4(status.pose.pose());
            loadMat3(status.pose.normal());
            loadLightAndVisible(status);
        }
        boneStatusUboBuffer.flip();
    }

    public static boolean isCurrentSupportGcrRender() {
        ShaderInstance shader = getShader();
        if (shader == null) {
            return false;
        }

        return GL20.glGetUniformLocation(
                shader.getId(),
                "gcrDoTransformOverride"
        ) != -1;
    }

    public void draw(int vertexCount, int indices) {
        VertexBufferAccessor accessor = (VertexBufferAccessor) rawDataVertexBuffer;
        GlStateManager._drawElements(
                accessor.getMode().asGLMode,
                vertexCount,
                accessor.invokeGetIndexType().asGLType,
                (long) indices * accessor.invokeGetIndexType().bytes);
    }

    public int getIndexByteSize() {
        return ((VertexBufferAccessor) rawDataVertexBuffer).invokeGetIndexType().bytes;
    }

    public boolean checkShaderUbo(int shaderProgramId) {
        if (uboId == -1) {
            return false;
        }
        if (shaderProgramId == 0) {
            return false;
        }

        if (lastKnownProgramId != shaderProgramId) {
            int blockIndex = GL31.glGetUniformBlockIndex(shaderProgramId, "GcrBoneUBO");
            if (blockIndex == GL31.GL_INVALID_INDEX) {
                lastKnownProgramId = -1;
                return false;
            }
            GL31.glUniformBlockBinding(shaderProgramId, blockIndex, UBO_BINDING_POINT);
            lastKnownProgramId = shaderProgramId;
        }
        GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, UBO_BINDING_POINT, uboId);
        return true;
    }

    public void uploadUbo() {
        glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, boneStatusUboBuffer);
        glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    protected void afterUniformLoaded(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass, float partialTicks) {}

    private void loadLightAndVisible(BoneRenderStatus status) {
        int light = status.lightmapUV;
        int u = light & '\uffff';
        int v = light >> 16 & '\uffff';
        boneStatusUboBuffer.put((float) u);
        boneStatusUboBuffer.put((float) v);
        boneStatusUboBuffer.put(status.visible ? 1.0f : 0.0f);
        boneStatusUboBuffer.put(0.0f);//padding
    }

    private void loadMat3(Matrix3f mat) {
        boneStatusUboBuffer.put(mat.m00);
        boneStatusUboBuffer.put(mat.m01);
        boneStatusUboBuffer.put(mat.m02);
        boneStatusUboBuffer.put(0.0f);//padding
        boneStatusUboBuffer.put(mat.m10);
        boneStatusUboBuffer.put(mat.m11);
        boneStatusUboBuffer.put(mat.m12);
        boneStatusUboBuffer.put(0.0f);//padding
        boneStatusUboBuffer.put(mat.m20);
        boneStatusUboBuffer.put(mat.m21);
        boneStatusUboBuffer.put(mat.m22);
        boneStatusUboBuffer.put(0.0f);//padding
    }

    protected void loadMat4(Matrix4f mat) {
        boneStatusUboBuffer.put(mat.m00());
        boneStatusUboBuffer.put(mat.m01());
        boneStatusUboBuffer.put(mat.m02());
        boneStatusUboBuffer.put(mat.m03());
        boneStatusUboBuffer.put(mat.m10());
        boneStatusUboBuffer.put(mat.m11());
        boneStatusUboBuffer.put(mat.m12());
        boneStatusUboBuffer.put(mat.m13());
        boneStatusUboBuffer.put(mat.m20());
        boneStatusUboBuffer.put(mat.m21());
        boneStatusUboBuffer.put(mat.m22());
        boneStatusUboBuffer.put(mat.m23());
        boneStatusUboBuffer.put(mat.m30());
        boneStatusUboBuffer.put(mat.m31());
        boneStatusUboBuffer.put(mat.m32());
        boneStatusUboBuffer.put(mat.m33());
    }

    public void resetPose() {
        rootBone.resetPoseAll();
    }
    protected void updateBoneRenderStatus(Bone root, PoseStack poseStack, int light) {
        if (root.visible) {
            poseStack.pushPose();
            root.translateAndRotate(poseStack);
            root.updateRenderStatus(poseStack, light);
            renderingVertexCount += root.vertexCount;
            for (Bone child : root.children.values()) {
                updateBoneRenderStatus(child, poseStack, light);
            }
            poseStack.popPose();
        }
    }

    protected void onBoneTouched(Bone bone) {}

    public void updateBoneRenderStatus(PoseStack poseStack, int light) {
        this.updateBoneRenderStatus(rootBone, poseStack, light);
    }


    public ResourceLocation getDebugName() {
        return debugName;
    }

}
