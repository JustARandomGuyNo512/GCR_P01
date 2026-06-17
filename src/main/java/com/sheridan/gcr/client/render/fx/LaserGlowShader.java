package com.sheridan.gcr.client.render.fx;

import com.sheridan.gcr.GCR;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;

@OnlyIn(Dist.CLIENT)
public class LaserGlowShader {
    public static int programId = -1;
    public static int modelViewMatLoc = -1;
    public static int projMatLoc = -1;
    public static int glowColorLoc = -1;

    public static int vaoId = -1;
    public static int vboId = -1;
    public static int posLoc = -1;
    public static int uvLoc = -1;

    // 新增：记录 3D 球体的顶点总数
    public static int vertexCount = 0;

    private static boolean isOK = false;

    public static void init() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation vshPath = GCR.RL("shaders/core/laser_glow.vsh");
        ResourceLocation fshPath = GCR.RL("shaders/core/laser_glow.fsh");

        String vshSource = load(manager, vshPath);
        String fshSource = load(manager, fshPath);

        if (vshSource == null || fshSource == null) return;

        int vsh = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vsh, vshSource);
        glCompileShader(vsh);
        if (GL20.glGetShaderi(vsh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(vsh);
            return;
        }

        int fsh = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsh, fshSource);
        glCompileShader(fsh);
        if (GL20.glGetShaderi(fsh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(vsh);
            GL20.glDeleteShader(fsh);
            return;
        }

        programId = glCreateProgram();
        glAttachShader(programId, vsh);
        glAttachShader(programId, fsh);
        glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            programId = -1;
            return;
        }
        glDeleteShader(vsh);
        glDeleteShader(fsh);

        modelViewMatLoc = GL20.glGetUniformLocation(programId, "ModelViewMat");
        projMatLoc = GL20.glGetUniformLocation(programId, "ProjMat");
        glowColorLoc = GL20.glGetUniformLocation(programId, "GlowColor");

        posLoc = GL20.glGetAttribLocation(programId, "Position");
        uvLoc = GL20.glGetAttribLocation(programId, "UV0");

        float[] vertices = generateSphereVertices(12, 12);
        vertexCount = vertices.length / 5;

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES;
        GL20.glVertexAttribPointer(posLoc, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(posLoc);
        GL20.glVertexAttribPointer(uvLoc, 2, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(uvLoc);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        isOK = programId != -1;
    }

    // 过程式 3D 经纬度球体网格生成器
    private static float[] generateSphereVertices(int segments, int rings) {
        List<Float> list = new ArrayList<>();
        for (int r = 0; r < rings; r++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) r / rings);
            float z0  = (float) Math.sin(lat0);
            float r0  = (float) Math.cos(lat0);

            float lat1 = (float) Math.PI * (-0.5f + (float) (r + 1) / rings);
            float z1  = (float) Math.sin(lat1);
            float r1  = (float) Math.cos(lat1);

            for (int c = 0; c < segments; c++) {
                float lng0 = (float) (2 * Math.PI * (float) c / segments);
                float x00 = (float) Math.cos(lng0) * r0;
                float y00 = (float) Math.sin(lng0) * r0;

                float lng1 = (float) (2 * Math.PI * (float) (c + 1) / segments);
                float x10 = (float) Math.cos(lng1) * r0;
                float y10 = (float) Math.sin(lng1) * r0;

                float x01 = (float) Math.cos(lng0) * r1;
                float y01 = (float) Math.sin(lng0) * r1;

                float x11 = (float) Math.cos(lng1) * r1;
                float y11 = (float) Math.sin(lng1) * r1;

                float u0 = (float) c / segments;
                float u1 = (float) (c + 1) / segments;
                float v0 = (float) r / rings;
                float v1 = (float) (r + 1) / rings;

                // 三角形 1
                addVertex(list, x00, y00, z0, u0, v0);
                addVertex(list, x10, y10, z0, u1, v0);
                addVertex(list, x01, y01, z1, u0, v1);
                // 三角形 2
                addVertex(list, x10, y10, z0, u1, v0);
                addVertex(list, x11, y11, z1, u1, v1);
                addVertex(list, x01, y01, z1, u0, v1);
            }
        }
        float[] res = new float[list.size()];
        for (int i = 0; i < list.size(); i++) res[i] = list.get(i);
        return res;
    }

    private static void addVertex(List<Float> list, float x, float y, float z, float u, float v) {
        list.add(x); list.add(y); list.add(z);
        list.add(u); list.add(v);
    }

    public static boolean isOK() { return isOK; }

    private static String load(ResourceManager manager, ResourceLocation path) {
        return manager.getResource(path).map(res -> {
            try (InputStream in = res.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) { return null; }
        }).orElse(null);
    }
}