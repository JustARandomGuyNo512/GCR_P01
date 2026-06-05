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

import static org.lwjgl.opengl.GL20.*;

@OnlyIn(Dist.CLIENT)
public class IrisGunPostShader {
    public static int programId = -1;
    public static int muzzleLightContributionSamplerLoc = -1;

    public static int vaoId = -1;
    public static int vboId = -1;
    public static int posLoc = -1;
    public static int uvLoc = -1;

    private static boolean isOK = false;

    public static void init() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation vshPath = GCR.RL("shaders/core/iris_gun_post.vsh");
        ResourceLocation fshPath = GCR.RL("shaders/core/iris_gun_post.fsh");

        String vshSource = load(manager, vshPath);
        String fshSource = load(manager, fshPath);

        if (vshSource == null || fshSource == null) {
            System.out.println("Failed to load shader files IrisGunPostShader.");
            return;
        }

        int vsh = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vsh, vshSource);
        glCompileShader(vsh);

        if (GL20.glGetShaderi(vsh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(vsh, 512);
            System.out.println("failed to compile [IrisGunPostShader] VSH! \n" + log);
            GL20.glDeleteShader(vsh);
            isOK = false;
            return;
        }


        int fsh = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsh, fshSource);
        glCompileShader(fsh);
        if (GL20.glGetShaderi(fsh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(fsh, 512);
            System.out.println("failed to compile [IrisGunPostShader] FSH! \n" + log);
            GL20.glDeleteShader(vsh); // 清理 VSH
            GL20.glDeleteShader(fsh);
            isOK = false;
            return;
        }

        programId = glCreateProgram();
        glAttachShader(programId, vsh);
        glAttachShader(programId, fsh);
        glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId, 512);
            System.out.println("failed to link [IrisGunPostShader] Program! \n" + log);
            programId = -1;
            isOK = false;
            return;
        }


        glDeleteShader(vsh);
        glDeleteShader(fsh);


        muzzleLightContributionSamplerLoc = GL20.glGetUniformLocation(programId, "MuzzleLightContributionSampler");


        posLoc = GL20.glGetAttribLocation(programId, "Position");
        uvLoc = GL20.glGetAttribLocation(programId, "UV0");
        float[] vertices = {
                -1.0f, -1.0f, 0.0f,  0.0f, 0.0f, // 左下
                1.0f, -1.0f, 0.0f,  1.0f, 0.0f, // 右下
                -1.0f,  1.0f, 0.0f,  0.0f, 1.0f, // 左上
                1.0f,  1.0f, 0.0f,  1.0f, 1.0f  // 右上
        };
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

        System.out.println("IrisGunPostShader init with: \n" + print());
    }

    public static String print() {
        return "IrisGunPostShader{\n" +
                "programId=" + programId +
                ", \nMuzzleLightContributionSampler=" + muzzleLightContributionSamplerLoc +
                ", \nvaoId=" + vaoId +
                ", \nvboId=" + vboId +
                ", \nposLoc=" + posLoc +
                ", \nuvLoc=" + uvLoc +
                "\n}";
    }

    public static boolean isOK() {
        return isOK;
    }

    private static String load(ResourceManager manager, ResourceLocation path) {
        return manager.getResource(path).map(res -> {
            try (InputStream in = res.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return null;
            }
        }).orElse(null);
    }
}
