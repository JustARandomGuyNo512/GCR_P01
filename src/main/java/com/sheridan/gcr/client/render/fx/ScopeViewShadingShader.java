package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
public class ScopeViewShadingShader {

    public static int programId = -1;

    // Uniform locations
    public static int uResolutionLoc = -1;
    public static int uLensCenterLoc = -1;
    public static int uLensRadiusLoc = -1;
    public static int uEyeOffsetLoc = -1;
    public static int uEyeDistanceLoc = -1;
    public static int uSensitivityLoc = -1;
    public static int uInnerFadeLoc = -1;
    public static int uOuterFadeLoc = -1;
    public static int uVignettePowerLoc = -1;
    public static int uDistortionStrengthLoc = -1;
    public static int uDistortionFalloffLoc = -1;
    public static int uGlassColorLoc = -1;
    public static int uGlassMixLoc = -1;
    public static int uSceneColorLoc = -1;
    public static int uTexMagnificationLoc = -1;

    private static TextureTarget sceneColorTarget;

    public static int captureSceneColor() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (sceneColorTarget == null) {
            sceneColorTarget = new TextureTarget(main.width, main.height, false, true);
            sceneColorTarget.setFilterMode(GL11.GL_LINEAR);
        } else if (sceneColorTarget.width != main.width || sceneColorTarget.height != main.height) {
            sceneColorTarget.resize(main.width, main.height, true);
            sceneColorTarget.setFilterMode(GL11.GL_LINEAR);
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneColorTarget.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height,
                0, 0, main.width, main.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);
        return sceneColorTarget.getColorTextureId();
    }


    // fullscreen quad
    public static int vaoId = -1;
    public static int vboId = -1;
    public static int posLoc = -1;
    public static int uvLoc = -1;

    private static boolean isOK = false;

    public static void init() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        ResourceLocation vshPath = GCR.RL("shaders/core/scope_view_shading.vsh");
        ResourceLocation fshPath = GCR.RL("shaders/core/scope_view_shading.fsh");

        String vshSource = load(manager, vshPath);
        String fshSource = load(manager, fshPath);

        if (vshSource == null || fshSource == null) {
            GCR.LOGGER.error("Failed to load Scope view shading shader.");
            return;
        }

        int vsh = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vsh, vshSource);
        glCompileShader(vsh);
        if (glGetShaderi(vsh, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("Failed to compile ScopeViewShading VSH:\n{}", glGetShaderInfoLog(vsh, 512));
            glDeleteShader(vsh);
            return;
        }

        int fsh = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsh, fshSource);
        glCompileShader(fsh);
        if (glGetShaderi(fsh, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("Failed to compile ScopeViewShading FSH:\n{}", glGetShaderInfoLog(fsh, 512));
            glDeleteShader(vsh);
            glDeleteShader(fsh);
            return;
        }

        programId = glCreateProgram();
        glAttachShader(programId, vsh);
        glAttachShader(programId, fsh);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("Failed to link ScopeViewShading Program:\n{}", glGetProgramInfoLog(programId, 512));
            programId = -1;
            return;
        }

        glDeleteShader(vsh);
        glDeleteShader(fsh);

        uResolutionLoc = glGetUniformLocation(programId, "uResolution");
        uLensCenterLoc = glGetUniformLocation(programId, "uLensCenter");
        uLensRadiusLoc = glGetUniformLocation(programId, "uLensRadius");
        uEyeOffsetLoc = glGetUniformLocation(programId, "uEyeOffset");
        uEyeDistanceLoc = glGetUniformLocation(programId, "uEyeDistance");
        uSensitivityLoc = glGetUniformLocation(programId, "uSensitivity");
        uInnerFadeLoc = glGetUniformLocation(programId, "uInnerFade");
        uOuterFadeLoc = glGetUniformLocation(programId, "uOuterFade");
        uVignettePowerLoc = glGetUniformLocation(programId, "uVignettePower");
        uDistortionStrengthLoc = glGetUniformLocation(programId, "uDistortionStrength");
        uDistortionFalloffLoc = glGetUniformLocation(programId, "uDistortionFalloff");
        uGlassColorLoc = glGetUniformLocation(programId, "uGlassColor");
        uGlassMixLoc = glGetUniformLocation(programId, "uGlassMix");
        uSceneColorLoc = glGetUniformLocation(programId, "uSceneColor");
        uTexMagnificationLoc = glGetUniformLocation(programId, "uTexMagnification");

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

        GCR.LOGGER.info("ScopeViewShadingShader init: \n{}", print());
    }

    public static String print() {
        return "ScopeViewShadingShader{\n" +
                "programId=" + programId +
                "\nuResolutionLoc=" + uResolutionLoc +
                "\nuLensCenterLoc=" + uLensCenterLoc +
                "\nuLensRadiusLoc=" + uLensRadiusLoc +
                "\nuEyeOffsetLoc=" + uEyeOffsetLoc +
                "\nuEyeDistanceLoc=" + uEyeDistanceLoc +
                "\nuSensitivityLoc=" + uSensitivityLoc +
                "\nuInnerFadeLoc=" + uInnerFadeLoc +
                "\nuOuterFadeLoc=" + uOuterFadeLoc +
                "\nuVignettePowerLoc=" + uVignettePowerLoc +
                "\nposLoc=" + posLoc +
                "\nuvLoc=" + uvLoc +
                "\nuTexMagnification=" + uTexMagnificationLoc +
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
