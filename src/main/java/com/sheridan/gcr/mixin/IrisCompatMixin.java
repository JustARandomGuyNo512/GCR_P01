package com.sheridan.gcr.mixin;

import com.sheridan.gcr.GCR;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher")
public class IrisCompatMixin {

    @Inject(method = "transform", at = @At("TAIL"), remap = false)
    private static void test1(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment, Parameters parameters, CallbackInfoReturnable<Map<PatchShaderType, String>> cir) {
        if (parameters.patch == Patch.VANILLA) {//shadow_entities_cutout  //entities_cutout_diffuse //hand_cutout_diffuse
            if ("shadow_entities_cutout".equals(name) || "entities_cutout_diffuse".equals(name) || "hand_cutout_diffuse".equals(name)) {
                Map<PatchShaderType, String> returnValue = cir.getReturnValue();
                String vsh = returnValue.get(PatchShaderType.VERTEX);
                vsh = injectUniformsAndTransformLogic(vsh);
                if (GCR.IS_DEVELOPMENT) {
                    System.out.println("Injecting uniforms and logic into: " + name);
                    System.out.println(vsh);
                    System.out.println("=================================================\n");
                }
                returnValue.put(PatchShaderType.VERTEX, vsh);
            }
        }
    }


    private static String injectUniformsAndTransformLogic(String vsh) {

        if (vsh.contains("uniform int gcrDoTransformOverride;")) {
            return vsh;
        }

        int versionIndex = vsh.indexOf("#version");
        if (versionIndex == -1) {
            System.err.println("Shader is missing #version directive!");
            return vsh;
        }

        String uniforms = "";
        if (!vsh.contains("uniform int gcrDoTransformOverride;")) {
            uniforms += "uniform int gcrDoTransformOverride;\n";
        }

        if (!vsh.contains("struct GcrBoneData {mat4 TransMat;mat4 PacketNormalLightVisible;};")) {
            uniforms += "struct GcrBoneData {mat4 TransMat;mat4 PacketNormalLightVisible;};\n";
        }

        if (!vsh.contains("layout (std140) uniform GcrBoneUBO {GcrBoneData gcrBones[128];};")) {
            uniforms += "layout (std140) uniform GcrBoneUBO {GcrBoneData gcrBones[128];};\n";
        }

        if (!vsh.contains("ivec2 gcrMixedLightmap")) {
            uniforms += "ivec2 gcrMixedLightmap;\n";
        }

        if (!vsh.contains("ivec2 gcrOverrideOverlay;")) {
            uniforms += "ivec2 gcrOverrideOverlay;\n";
        }

        if (!vsh.contains("vec3 gcrTransformedNormal")) {
            uniforms += "vec3 gcrTransformedNormal;\n";
        }

        if (!vsh.contains("vec4 gcrTransformedPos")) {
            uniforms += "vec4 gcrTransformedPos;\n";
        }

        int versionLineEnd = vsh.indexOf('\n', versionIndex);
        String beforeVersionEnd = vsh.substring(0, versionLineEnd + 1);
        String afterVersionEnd = vsh.substring(versionLineEnd + 1);
        String s = beforeVersionEnd + uniforms + afterVersionEnd;

        int mainIndex = s.indexOf("void main()");
        if (mainIndex == -1) return vsh; // 没找到 main，返回原样

        int braceOpen = s.indexOf("{", mainIndex);
        if (braceOpen == -1) return vsh;

        String beforeMainBody = s.substring(0, braceOpen + 1); // 包含 {
        String afterMainBody = s.substring(braceOpen + 1);

        String injectedCode = """

                gcrOverrideOverlay = iris_UV1;
                gcrTransformedNormal = iris_Normal;
                gcrTransformedPos = vec4(iris_Position, 1.0);
                gcrMixedLightmap = iris_UV2;

                if (gcrDoTransformOverride == 1) {
                    int boneId = iris_UV1.x;
                    GcrBoneData data = gcrBones[boneId];

                    bool visible = data.PacketNormalLightVisible[3][2] > 0.5;

                    if (!visible) {
                        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
                        return;
                    }

                    gcrTransformedNormal = mat3(data.PacketNormalLightVisible) * iris_Normal;
                    gcrTransformedNormal = normalize(gcrTransformedNormal);
                    gcrTransformedPos = data.TransMat * gcrTransformedPos;

                    gcrMixedLightmap = ivec2(data.PacketNormalLightVisible[3][0], data.PacketNormalLightVisible[3][1]);
                    gcrOverrideOverlay = ivec2(0, 10);
                }

                """;

        String program = beforeMainBody + injectedCode + afterMainBody;
        String[] split = program.split("\n");
        for (int i = 0; i < split.length; i++) {
            if (!(split[i].startsWith("in") ||
                    "gcrTransformedNormal = mat3(data.PacketNormalLightVisible) * iris_Normal".equals(split[i]) ||
                    "gcrTransformedNormal = iris_Normal;".equals(split[i]))) {
                split[i] = split[i].replaceAll("\\biris_Normal\\b(?!\\w)", "gcrTransformedNormal");
            }
            if (!("gcrTransformedPos = vec4(iris_Position, 1.0);".equals(split[i]))) {
                split[i] = split[i].replaceAll("vec4\\s*\\(\\s*iris_Position\\s*,\\s*1\\.0\\s*f?\\s*\\)", "gcrTransformedPos");
            }
            if (!(split[i].startsWith("in") || "gcrMixedLightmap = iris_UV2;".equals(split[i]))) {
                split[i] = split[i].replaceAll("iris_UV2", "gcrMixedLightmap");
            }
            if (!(split[i].startsWith("in") || "gcrOverrideOverlay = iris_UV1;".equals(split[i]))) {
                split[i] = split[i].replaceAll("iris_UV1", "gcrOverrideOverlay");
            }
        }
        program = String.join("\n", split);
        return program;
    }

}