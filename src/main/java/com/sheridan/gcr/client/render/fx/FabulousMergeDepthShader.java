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
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

@OnlyIn(Dist.CLIENT)
public class FabulousMergeDepthShader {

    public static int programId = -1;

    // Uniform locations
    public static int mainDepthLoc = -1;
    public static int translucentDepthLoc = -1;
    public static int itemDepthLoc = -1;
    public static int particlesDepthLoc = -1;
    public static int cloudsDepthLoc = -1;
    public static int weatherDepthLoc = -1;

    // fullscreen quad
    public static int vaoId = -1;
    public static int vboId = -1;
    public static int posLoc = -1;
    public static int uvLoc = -1;

    private static boolean isOK = false;

    public static void init() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        ResourceLocation vshPath = GCR.RL("shaders/core/muzzle_flash_env.vsh"); // 复用你的全屏 VSH
        ResourceLocation fshPath = GCR.RL("shaders/core/fabulous_merge_depth.fsh");

        String vshSource = load(manager, vshPath);
        String fshSource = load(manager, fshPath);

        if (vshSource == null || fshSource == null) {
            GCR.LOGGER.error("Failed to load Fabulous depth merge shader.");
            return;
        }

        int vsh = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vsh, vshSource);
        glCompileShader(vsh);
        if (glGetShaderi(vsh, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("Failed to compile FabulousMergeDepth VSH:\n{}", glGetShaderInfoLog(vsh, 512));
            glDeleteShader(vsh);
            return;
        }

        int fsh = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsh, fshSource);
        glCompileShader(fsh);
        if (glGetShaderi(fsh, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("Failed to compile FabulousMergeDepth FSH:\n{}", glGetShaderInfoLog(fsh, 512));
            glDeleteShader(vsh);
            glDeleteShader(fsh);
            return;
        }

        programId = glCreateProgram();
        glAttachShader(programId, vsh);
        glAttachShader(programId, fsh);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("Failed to link FabulousMergeDepth Program:\n{}", glGetProgramInfoLog(programId, 512));
            programId = -1;
            return;
        }

        glDeleteShader(vsh);
        glDeleteShader(fsh);

        mainDepthLoc = glGetUniformLocation(programId, "MainDepth");
        translucentDepthLoc = glGetUniformLocation(programId, "TranslucentDepth");
        itemDepthLoc = glGetUniformLocation(programId, "ItemDepth");
        particlesDepthLoc = glGetUniformLocation(programId, "ParticlesDepth");
        cloudsDepthLoc = glGetUniformLocation(programId, "CloudsDepth");
        weatherDepthLoc = glGetUniformLocation(programId, "WeatherDepth");

        posLoc = glGetAttribLocation(programId, "Position");
        uvLoc  = glGetAttribLocation(programId, "UV0");

        float[] vertices = {
                -1f, -1f, 0f,  0f, 0f,
                1f, -1f, 0f,  1f, 0f,
                -1f,  1f, 0f,  0f, 1f,
                1f,  1f, 0f,  1f, 1f
        };

        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES;

        glVertexAttribPointer(posLoc, 3, GL11.GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(posLoc);

        glVertexAttribPointer(uvLoc, 2, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(uvLoc);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        isOK = programId != -1;

        GCR.LOGGER.info("FabulousMergeDepthShader init: \n{}", print());
    }

    public static String print() {
        return "FabulousMergeDepthShader{\n" +
                "programId=" + programId +
                ",\n mainDepthLoc=" + mainDepthLoc +
                ",\n translucentDepthLoc=" + translucentDepthLoc +
                ",\n itemDepthLoc=" + itemDepthLoc +
                ",\n particlesDepthLoc=" + particlesDepthLoc +
                ",\n cloudsDepthLoc=" + cloudsDepthLoc +
                ",\n weatherDepthLoc=" + weatherDepthLoc +
                ",\n vaoId=" + vaoId +
                ",\n vboId=" + vboId +
                ",\n posLoc=" + posLoc +
                ",\n uvLoc=" + uvLoc +
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
