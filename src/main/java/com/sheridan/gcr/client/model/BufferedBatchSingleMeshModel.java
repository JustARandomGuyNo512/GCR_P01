package com.sheridan.gcr.client.model;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.mixin.VertexBufferAccessor;
import net.minecraft.client.Minecraft;
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
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BufferedBatchSingleMeshModel {
    protected CompatType compatType = CompatType.VANILLA;
    private static final int UBO_ELEMENT_SIZE = 16 * 2;
    private static final int UBO_BINDING_POINT = 0;
    private static final int MAX_BONES = 128;

    public static final int COMPILE_LIGHT = LightTexture.pack(15, 15);

    private final ResourceLocation name;
    private final int repeat; // 最大副本数

    private float[] singlePositions;
    private float[] singleNormals;
    private float[] singleUvs;
    private int singleVertexCount;

    // GPU 相关
    protected VertexBuffer rawDataVertexBuffer;
    protected FloatBuffer boneStatusUboBuffer;
    private int uboId = -1;
    private int lastKnownProgramId = -1;

    protected static class InstanceStatus {
        public PoseStack.Pose pose;
        public int lightmapUV;
        public boolean visible;

        public InstanceStatus() {
            this.pose = new PoseStack().last();
            this.visible = false;
            this.lightmapUV = COMPILE_LIGHT;
        }
    }

    private final InstanceStatus[] instanceStatuses;
    private int instanceCount = 0;

    protected int totalVertexCount = 0;
    private RenderType renderType;

    public BufferedBatchSingleMeshModel(MeshModelData singleMesh, ResourceLocation name, int repeat) {
        if (repeat <= 0) {
            throw new IllegalArgumentException("repeat must > 0");
        }
        if (repeat > MAX_BONES) {
            throw new IllegalArgumentException("repeat must <= MAX_BONES");
        }
        this.name = name;
        this.repeat = repeat;
        this.instanceStatuses = new InstanceStatus[repeat];
        for (int i = 0; i < repeat; i++) this.instanceStatuses[i] = new InstanceStatus();
        readSingleMesh(singleMesh);
    }

    // 读取单个 mesh 的顶点到 CPU 数组（保持与之前代码类似格式）
    private void readSingleMesh(MeshModelData root) {
        List<Vertex> vertices = root.getVertices();
        this.singleVertexCount = vertices.size();
        this.singlePositions = new float[singleVertexCount * 3];
        this.singleNormals = new float[singleVertexCount * 3];
        this.singleUvs = new float[singleVertexCount * 2];
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            int b = i * 3;
            singlePositions[b] = v.x; singlePositions[b + 1] = v.y; singlePositions[b + 2] = v.z;
            singleNormals[b] = v.normalX; singleNormals[b + 1] = v.normalY; singleNormals[b + 2] = v.normalZ;
            singleUvs[i * 2] = v.u; singleUvs[i * 2 + 1] = v.v;
        }
    }

    // compile：在 VBO 中将 single mesh 重复写 repeat 次（每份标记不同的 UV1.x = instanceIndex）
    public void compile(RenderType type) {
        if (rawDataVertexBuffer != null) {
            return;
        }
        this.renderType = type;
        rawDataVertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder rawBuilderBuffer = new ByteBufferBuilder(1024 * 64);
        BufferBuilder rawBuilder = new BufferBuilder(rawBuilderBuffer, type.mode, type.format);
        PoseStack.Pose dummyPose = new PoseStack().last();
        for (int inst = 0; inst < repeat; inst++) {
            for (int i = 0; i < singleVertexCount; i++) {
                int posIndex = i * 3;
                rawBuilder.addVertex(dummyPose, singlePositions[posIndex], singlePositions[posIndex + 1], singlePositions[posIndex + 2])
                        .setColor(1f, 1f, 1f, 1f)
                        .setUv(singleUvs[i * 2], singleUvs[i * 2 + 1])
                        .setUv1(inst, 0)
                        .setLight(COMPILE_LIGHT)
                        .setNormal(dummyPose, singleNormals[posIndex], singleNormals[posIndex + 1], singleNormals[posIndex + 2]);
            }
        }

        checkOrCreateUBO();

        MeshData rawData = rawBuilder.build();
        if (rawData != null) {
            if (type.sortOnUpload()) {
                rawData.sortQuads(rawBuilderBuffer, RenderSystem.getVertexSorting());
            }
            rawDataVertexBuffer.bind();
            rawDataVertexBuffer.upload(rawData);
            VertexBuffer.unbind();
            rawBuilderBuffer.discard();
            rawBuilderBuffer.close();
        }

        totalVertexCount = singleVertexCount * repeat;
        if (boneStatusUboBuffer == null) {
            boneStatusUboBuffer = BufferUtils.createFloatBuffer(UBO_ELEMENT_SIZE * repeat);
        }
        initUboBufferWithIdentity();
    }

    public void setRenderType(RenderType type, boolean recompile) {
        this.renderType = type;
        if (recompile) {
            compile(type);
        }
    }


    private void initUboBufferWithIdentity() {
        boneStatusUboBuffer.clear();
        for (int i = 0; i < repeat; i++) {
            loadMat4Identity(boneStatusUboBuffer); // 16 floats
            loadMat3Identity(boneStatusUboBuffer); // 12 floats (含 padding)
            loadLightAndVisible(boneStatusUboBuffer, COMPILE_LIGHT, false); // 4 floats
        }
        boneStatusUboBuffer.flip(); // 准备好
    }

    private void checkOrCreateUBO() {
        if (uboId != -1) {
            return;
        }
        uboId = GL15.glGenBuffers();
        int bufferSizeInFloats = repeat * UBO_ELEMENT_SIZE;
        long bytes = (long) bufferSizeInFloats * Float.BYTES;
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, bytes, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public int addInstance(PoseStack.Pose pose, int lightmapUV) {
        if (instanceCount >= repeat) {
            return -1;
        }
        InstanceStatus s = instanceStatuses[instanceCount];
        s.pose = pose.copy();
        s.lightmapUV = lightmapUV;
        s.visible = true;
        instanceCount ++;
        return instanceCount;
    }

    public void resetInstances() {
        for (int i = 0; i < instanceCount; i++) {
            instanceStatuses[i].visible = false;
        }
        instanceCount = 0;
        if (boneStatusUboBuffer != null) {
            boneStatusUboBuffer.clear();
        }
    }

    // 释放资源
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
        lastKnownProgramId = -1;
    }

    protected void prepareUbo() {
        boneStatusUboBuffer.clear(); // 重置 position = 0
        for (int i = 0; i < repeat; i++) {
            InstanceStatus s = instanceStatuses[i];
            if (s.visible) {
                loadMat4(boneStatusUboBuffer, s.pose.pose());
                loadMat3(boneStatusUboBuffer, s.pose.normal());
                loadLightAndVisible(boneStatusUboBuffer, s.lightmapUV, true);
            } else {
                // 不可见：跳过矩阵写入，只写控制位
                // 1. 跳过 Mat4 (16) + Mat3 (12) = 28 个 float
                int currentPos = boneStatusUboBuffer.position();
                boneStatusUboBuffer.position(currentPos + 28);
                loadLightAndVisible(boneStatusUboBuffer, COMPILE_LIGHT, false);
            }
        }
        boneStatusUboBuffer.flip();
    }

    private void loadLightAndVisible(FloatBuffer buf, int light, boolean visible) {
        int u = light & '\uffff';
        int v = light >> 16 & '\uffff';
        buf.put((float) u);
        buf.put((float) v);
        buf.put(visible ? 1.0f : 0.0f);
        buf.put(0.0f);
    }

    private void loadMat3(FloatBuffer buf, Matrix3f mat) {
        buf.put(mat.m00); buf.put(mat.m01); buf.put(mat.m02); buf.put(0.0f);
        buf.put(mat.m10); buf.put(mat.m11); buf.put(mat.m12); buf.put(0.0f);
        buf.put(mat.m20); buf.put(mat.m21); buf.put(mat.m22); buf.put(0.0f);
    }

    private void loadMat4(FloatBuffer buf, Matrix4f mat) {
        buf.put(mat.m00()); buf.put(mat.m01()); buf.put(mat.m02()); buf.put(mat.m03());
        buf.put(mat.m10()); buf.put(mat.m11()); buf.put(mat.m12()); buf.put(mat.m13());
        buf.put(mat.m20()); buf.put(mat.m21()); buf.put(mat.m22()); buf.put(mat.m23());
        buf.put(mat.m30()); buf.put(mat.m31()); buf.put(mat.m32()); buf.put(mat.m33());
    }

    private void loadMat3Identity(FloatBuffer buf) {
        buf.put(1f); buf.put(0f); buf.put(0f); buf.put(0f);
        buf.put(0f); buf.put(1f); buf.put(0f); buf.put(0f);
        buf.put(0f); buf.put(0f); buf.put(1f); buf.put(0f);
    }
    private void loadMat4Identity(FloatBuffer buf) {
        buf.put(1f); buf.put(0f); buf.put(0f); buf.put(0f);
        buf.put(0f); buf.put(1f); buf.put(0f); buf.put(0f);
        buf.put(0f); buf.put(0f); buf.put(1f); buf.put(0f);
        buf.put(0f); buf.put(0f); buf.put(0f); buf.put(1f);
    }

    // 绑定 UBO 并上传数据
    protected boolean checkShaderUbo(int shaderProgramId) {
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

    protected void uploadUbo() {
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, boneStatusUboBuffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    protected boolean updateCompatType() {
        CompatType prevType = Client.isUseIrisShader ? CompatType.IRIS : CompatType.VANILLA;
        if (compatType != prevType) {
            compatType = prevType;
            release();
            compile(renderType);
            System.out.println("Recompile for compat type: " + compatType + " vertex count: " + totalVertexCount);
            return false;
        }
        return true;
    }

    // render：一次 draw 所有 repeat 副本（shader 内使用 UV1.x 去索引 UBO）
    public void render(boolean isFirstPerson) {
        if (rawDataVertexBuffer != null && renderType != null) {
            if (!updateCompatType()) {
                return;
            }
            ShaderInstance shader = BufferedBoneMeshModel.getShader();
            if (shader == null) {
                return;
            }
            if (instanceCount == 0) {
                return;
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
            if (checkShaderUbo(shader.getId())) {
                shader.apply();
                prepareUbo();
                uploadUbo();
                if (compatType == CompatType.IRIS) {
                    renderInIris(shader, accessor, isFirstPerson, renderingShadowPass);
                } else if (compatType == CompatType.VANILLA) {
                    renderInVanilla(shader, accessor, isFirstPerson);
                }
                shader.clear();
                resetInstances();
            }
            VertexBuffer.unbind();
            renderType.clearRenderState();

        }
    }

    @Nullable
    public RenderType getRenderType() {
        return renderType;
    }

    protected void renderInIris(ShaderInstance shader, VertexBufferAccessor accessor, boolean isFirstPerson, boolean isShadowPass) {
        int irisOverride = GL20.glGetUniformLocation(shader.getId(), "gcrDoTransformOverride");
        if (irisOverride == -1) {
            return;
        }
        GL20.glUniform1i(irisOverride, 1);
        renderInner(accessor, shader, isFirstPerson, isShadowPass);
        GL20.glUniform1i(irisOverride, 0);
    }

    protected void renderInVanilla(ShaderInstance shader, VertexBufferAccessor accessor, boolean isFirstPerson) {
        renderInner(accessor, shader, isFirstPerson, false);
    }

    protected void renderInner(VertexBufferAccessor accessor, ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass) {
        afterUniformLoaded(shader, isFirstPerson, isShadowPass);
        GlStateManager._drawElements(
                accessor.getMode().asGLMode,
                totalVertexCount,
                accessor.invokeGetIndexType().asGLType,
                0);
    }

    protected void afterUniformLoaded(ShaderInstance shader, boolean isFirstPerson, boolean isShadowPass) {}

    public ResourceLocation getName() { return name; }
}
