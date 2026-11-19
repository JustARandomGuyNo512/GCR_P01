package com.sheridan.gcr.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

@OnlyIn(Dist.CLIENT)
public class SdfShader {
    private static int programId = -1;
    private static int vaoId = -1;
    private static int vboId = -1;

    private static int projMatLoc = -1;
    private static int modelViewMatLoc = -1;
    private static int rectLoc = -1;
    private static int sizeLoc = -1;
    private static int radiusLoc = -1;
    private static int fillColorLoc = -1;
    private static int borderColorLoc = -1;
    private static int borderThicknessLoc = -1;
    private static int shapeTypeLoc = -1;
    private static int arcAnglesLoc = -1;

    private static boolean initialized = false;
    private static boolean available = false;

    private static final String VSH_SOURCE = "#version 150\n" +
            "in vec2 Position;\n" +
            "uniform mat4 ProjMat;\n" +
            "uniform mat4 ModelViewMat;\n" +
            "uniform vec4 u_Rect;\n" +
            "out vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = Position;\n" +
            "    vec2 pos = u_Rect.xy + Position * u_Rect.zw;\n" +
            "    gl_Position = ProjMat * ModelViewMat * vec4(pos, 0.0, 1.0);\n" +
            "}\n";

    private static final String FSH_SOURCE = "#version 150\n" +
            "in vec2 v_TexCoord;\n" +
            "out vec4 FragColor;\n" +
            "uniform vec2 u_Size;\n" +
            "uniform float u_Radius;\n" +
            "uniform vec4 u_FillColor;\n" +
            "uniform vec4 u_BorderColor;\n" +
            "uniform float u_BorderThickness;\n" +
            "uniform int u_ShapeType;\n" +
            "uniform vec2 u_ArcAngles;\n" +
            "float sdRoundedBox(vec2 p, vec2 b, float r) {\n" +
            "    vec2 q = abs(p) - b + vec2(r);\n" +
            "    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;\n" +
            "}\n" +
            "float sdCircle(vec2 p, float r) {\n" +
            "    return length(p) - r;\n" +
            "}\n" +
            "float sdRing(vec2 p, float r, float th) {\n" +
            "    return abs(length(p) - r) - th * 0.5;\n" +
            "}\n" +
            "void main() {\n" +
            "    vec2 p = (v_TexCoord - 0.5) * u_Size;\n" +
            "    float dist = 0.0;\n" +
            "    if (u_ShapeType == 0) {\n" +
            "        dist = sdRoundedBox(p, u_Size * 0.5, u_Radius);\n" +
            "    } else if (u_ShapeType == 1) {\n" +
            "        dist = sdCircle(p, u_Radius);\n" +
            "    } else if (u_ShapeType == 2) {\n" +
            "        dist = sdRing(p, u_Radius, u_BorderThickness);\n" +
            "    } else if (u_ShapeType == 3) {\n" +
            "        float angle = atan(p.y, p.x);\n" +
            "        float r = length(p);\n" +
            "        float start = u_ArcAngles.x;\n" +
            "        float end = u_ArcAngles.y;\n" +
            "        float PI2 = 6.28318530718;\n" +
            "        float a = mod(angle - start, PI2);\n" +
            "        float total = mod(end - start, PI2);\n" +
            "        if (total <= 0.001 && end > start) total = PI2;\n" +
            "        float ringDist = abs(r - u_Radius) - u_BorderThickness * 0.5;\n" +
            "        if (a <= total) {\n" +
            "            dist = ringDist;\n" +
            "        } else {\n" +
            "            vec2 pStart = vec2(cos(start), sin(start)) * u_Radius;\n" +
            "            vec2 pEnd = vec2(cos(end), sin(end)) * u_Radius;\n" +
            "            dist = min(length(p - pStart), length(p - pEnd)) - u_BorderThickness * 0.5;\n" +
            "        }\n" +
            "    }\n" +
            "    float smoothing = 1.0;\n" +
            "    float fillAlpha = smoothstep(smoothing, -smoothing, dist);\n" +
            "    if (fillAlpha <= 0.001) discard;\n" +
            "    vec4 finalColor = u_FillColor;\n" +
            "    if (u_BorderThickness > 0.0 && u_ShapeType != 2 && u_ShapeType != 3) {\n" +
            "        float borderDist = dist + u_BorderThickness;\n" +
            "        float borderAlpha = smoothstep(-smoothing, smoothing, dist) * smoothstep(smoothing, -smoothing, borderDist);\n" +
            "        finalColor = mix(u_FillColor, u_BorderColor, clamp(borderAlpha * u_BorderColor.a, 0.0, 1.0));\n" +
            "    }\n" +
            "    finalColor.a *= fillAlpha;\n" +
            "    FragColor = finalColor;\n" +
            "}\n";

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            int vsh = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vsh, VSH_SOURCE);
            glCompileShader(vsh);
            if (glGetShaderi(vsh, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                glDeleteShader(vsh);
                return;
            }

            int fsh = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fsh, FSH_SOURCE);
            glCompileShader(fsh);
            if (glGetShaderi(fsh, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                glDeleteShader(vsh);
                glDeleteShader(fsh);
                return;
            }

            programId = glCreateProgram();
            glAttachShader(programId, vsh);
            glAttachShader(programId, fsh);
            glLinkProgram(programId);
            if (glGetProgrami(programId, GL_LINK_STATUS) == GL11.GL_FALSE) {
                programId = -1;
                return;
            }
            glDeleteShader(vsh);
            glDeleteShader(fsh);

            projMatLoc = glGetUniformLocation(programId, "ProjMat");
            modelViewMatLoc = glGetUniformLocation(programId, "ModelViewMat");
            rectLoc = glGetUniformLocation(programId, "u_Rect");
            sizeLoc = glGetUniformLocation(programId, "u_Size");
            radiusLoc = glGetUniformLocation(programId, "u_Radius");
            fillColorLoc = glGetUniformLocation(programId, "u_FillColor");
            borderColorLoc = glGetUniformLocation(programId, "u_BorderColor");
            borderThicknessLoc = glGetUniformLocation(programId, "u_BorderThickness");
            shapeTypeLoc = glGetUniformLocation(programId, "u_ShapeType");
            arcAnglesLoc = glGetUniformLocation(programId, "u_ArcAngles");

            float[] quadVertices = {
                    0.0f, 0.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f
            };
            FloatBuffer vb = BufferUtils.createFloatBuffer(quadVertices.length);
            vb.put(quadVertices).flip();

            vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);

            vboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vb, GL15.GL_STATIC_DRAW);

            int posLoc = glGetAttribLocation(programId, "Position");
            glVertexAttribPointer(posLoc, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);
            glEnableVertexAttribArray(posLoc);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);

            available = true;
        } catch (Exception e) {
            e.printStackTrace();
            available = false;
        }
    }

    public static boolean isAvailable() {
        if (!initialized) {
            init();
        }
        return available;
    }

    private static final float[] MAT_BUFFER = new float[16];

    public static void begin(PoseStack poseStack) {
        if (!isAvailable()) return;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        glUseProgram(programId);
        GL30.glBindVertexArray(vaoId);

        Matrix4f proj = RenderSystem.getProjectionMatrix();
        proj.get(MAT_BUFFER);
        glUniformMatrix4fv(projMatLoc, false, MAT_BUFFER);

        Matrix4f modelView = poseStack != null ? poseStack.last().pose() : RenderSystem.getModelViewMatrix();
        modelView.get(MAT_BUFFER);
        glUniformMatrix4fv(modelViewMatLoc, false, MAT_BUFFER);
    }

    public static void end() {
        if (!isAvailable()) return;
        GL30.glBindVertexArray(0);
        glUseProgram(0);
        GL11.glDepthMask(true);
    }

    public static void drawCircle(float cx, float cy, float radius, int fillColor, int borderColor, float borderWidth) {
        if (!isAvailable()) return;
        float pad = borderWidth + 2.0f;
        float x = cx - radius - pad;
        float y = cy - radius - pad;
        float size = (radius + pad) * 2.0f;

        glUniform4f(rectLoc, x, y, size, size);
        glUniform2f(sizeLoc, size, size);
        glUniform1f(radiusLoc, radius);
        setUniformColor(fillColorLoc, fillColor);
        setUniformColor(borderColorLoc, borderColor);
        glUniform1f(borderThicknessLoc, borderWidth);
        glUniform1i(shapeTypeLoc, 1);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int fillColor, int borderColor, float borderWidth) {
        if (!isAvailable()) return;
        float pad = borderWidth + 2.0f;
        float rx = x - pad;
        float ry = y - pad;
        float rw = w + pad * 2.0f;
        float rh = h + pad * 2.0f;

        glUniform4f(rectLoc, rx, ry, rw, rh);
        glUniform2f(sizeLoc, rw, rh);
        glUniform1f(radiusLoc, radius);
        setUniformColor(fillColorLoc, fillColor);
        setUniformColor(borderColorLoc, borderColor);
        glUniform1f(borderThicknessLoc, borderWidth);
        glUniform1i(shapeTypeLoc, 0);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    public static void drawRing(float cx, float cy, float radius, float thickness, int color) {
        if (!isAvailable()) return;
        float pad = thickness + 3.0f;
        float x = cx - radius - pad;
        float y = cy - radius - pad;
        float size = (radius + pad) * 2.0f;

        glUniform4f(rectLoc, x, y, size, size);
        glUniform2f(sizeLoc, size, size);
        glUniform1f(radiusLoc, radius);
        setUniformColor(fillColorLoc, color);
        glUniform4f(borderColorLoc, 0, 0, 0, 0);
        glUniform1f(borderThicknessLoc, thickness);
        glUniform1i(shapeTypeLoc, 2);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    public static void drawArc(float cx, float cy, float radius, float thickness, float startRad, float endRad, int color) {
        if (!isAvailable()) return;
        float pad = thickness + 3.0f;
        float x = cx - radius - pad;
        float y = cy - radius - pad;
        float size = (radius + pad) * 2.0f;

        glUniform4f(rectLoc, x, y, size, size);
        glUniform2f(sizeLoc, size, size);
        glUniform1f(radiusLoc, radius);
        setUniformColor(fillColorLoc, color);
        glUniform4f(borderColorLoc, 0, 0, 0, 0);
        glUniform1f(borderThicknessLoc, thickness);
        glUniform1i(shapeTypeLoc, 3);
        glUniform2f(arcAnglesLoc, startRad, endRad);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    private static void setUniformColor(int loc, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        glUniform4f(loc, r, g, b, a);
    }
}
