package com.sheridan.gcr.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;

/** A fixed std140 vec4[1024] bone block: eight vec4 values per bone. */
final class BoneUniformBuffer {
    static final int MAX_BONES = 128;
    static final int FLOATS_PER_BONE = 32;
    static final int BYTES_PER_BONE = FLOATS_PER_BONE * Float.BYTES;
    private static final int BINDING_POINT = 0;

    private final FloatBuffer data = BufferUtils.createFloatBuffer(MAX_BONES * FLOATS_PER_BONE);
    private int id = -1;
    private int lastProgram = -1;

    boolean create(int boneCount) {
        if (boneCount < 0 || boneCount > MAX_BONES || GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE) < MAX_BONES * BYTES_PER_BONE) return false;
        if (id == -1) {
            id = GL15.glGenBuffers();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, (long) MAX_BONES * BYTES_PER_BONE, GL15.GL_DYNAMIC_DRAW);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        }
        return true;
    }

    void beginWrite() { data.clear(); }

    void putBone(PoseStack.Pose pose, int light, boolean visible) {
        putMat4(pose.pose());
        Matrix3f normal = pose.normal();
        data.put(normal.m00).put(normal.m01).put(normal.m02).put(0);
        data.put(normal.m10).put(normal.m11).put(normal.m12).put(0);
        data.put(normal.m20).put(normal.m21).put(normal.m22).put(0);
        data.put((float) (light & 0xFFFF)).put((float) ((light >>> 16) & 0xFFFF));
        data.put(visible ? 1 : 0).put(0);
    }

    void putInvisibleBone(int light) {
        data.put(1).put(0).put(0).put(0);
        data.put(0).put(1).put(0).put(0);
        data.put(0).put(0).put(1).put(0);
        data.put(0).put(0).put(0).put(1);
        data.put(1).put(0).put(0).put(0);
        data.put(0).put(1).put(0).put(0);
        data.put(0).put(0).put(1).put(0);
        data.put((float) (light & 0xFFFF)).put((float) ((light >>> 16) & 0xFFFF)).put(0).put(0);
    }

    private void putMat4(Matrix4f mat) {
        data.put(mat.m00()).put(mat.m01()).put(mat.m02()).put(mat.m03());
        data.put(mat.m10()).put(mat.m11()).put(mat.m12()).put(mat.m13());
        data.put(mat.m20()).put(mat.m21()).put(mat.m22()).put(mat.m23());
        data.put(mat.m30()).put(mat.m31()).put(mat.m32()).put(mat.m33());
    }

    boolean bindProgram(int program) {
        if (id == -1 || program == 0) return false;
        if (lastProgram != program) {
            int block = GL31.glGetUniformBlockIndex(program, "GcrBoneUBO");
            if (block == GL31.GL_INVALID_INDEX || GL31.glGetActiveUniformBlocki(program, block, GL31.GL_UNIFORM_BLOCK_DATA_SIZE) > MAX_BONES * BYTES_PER_BONE) return false;
            GL31.glUniformBlockBinding(program, block, BINDING_POINT);
            lastProgram = program;
        }
        return true;
    }

    void uploadAndBind() {
        data.flip();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, BINDING_POINT, id);
    }

    void close() {
        if (!RenderSystem.isOnRenderThread()) throw new IllegalStateException("UBO must be released on the render thread");
        if (id != -1) GL15.glDeleteBuffers(id);
        id = -1;
        lastProgram = -1;
    }
}
