package com.sheridan.gcr.mixin;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.IrisExtendRT;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher")
public class IrisShaderCompatMixin {

    @Inject(method = "transform", at = @At("TAIL"), remap = false)
    private static void test1(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment, Parameters parameters, CallbackInfoReturnable<Map<PatchShaderType, String>> cir) {
        if (parameters.patch == Patch.VANILLA) {//shadow_entities_cutout  //entities_cutout_diffuse //hand_cutout_diffuse
            if ("shadow_entities_cutout".equals(name) || "entities_cutout_diffuse".equals(name) || "hand_cutout_diffuse".equals(name)) {
                Map<PatchShaderType, String> returnValue = cir.getReturnValue();
                boolean isFirstPerson = "hand_cutout_diffuse".equals(name);
                String vsh = returnValue.get(PatchShaderType.VERTEX);
                vsh = isFirstPerson ?
                        injectUniformsAndTransformLogicFP(vsh) :
                        injectUniformsAndTransformLogic(vsh);
                if (GCR.IS_DEVELOPMENT) {
                    System.out.println("Injecting uniforms and logic into vsh: " + name);
                    System.out.println(vsh);
                    System.out.println("=================================================\n");
                }
                if (isFirstPerson) {
                    String fsh = returnValue.get(PatchShaderType.FRAGMENT);
                    fsh = patchFPFsh(fsh);
                    if (GCR.IS_DEVELOPMENT) {
                        System.out.println("Injecting uniforms and logic into fsh: " + name);
                        System.out.println(fsh);
                        System.out.println("-------------------------------------------------\n");
                    }
                    returnValue.put(PatchShaderType.FRAGMENT, fsh);
                }
                returnValue.put(PatchShaderType.VERTEX, vsh);
            }
        }
    }


    private static String patchFPFsh(String fsh) {
        // 已经处理过：直接从已有声明里读出 location，赋值给 IrisExtendRT，不重复注入
        if (fsh.contains("gcrMuzzleLightContributions")) {
            int existingLoc = extractExistingLocation(fsh);
            IrisExtendRT.updateAttachmentLayout(existingLoc);
            return fsh;
        }

        int maxMRT = Math.min(
                GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS),
                GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS)
        );

        int chosenLoc = findFreeAttachmentLocation(fsh, maxMRT);
        if (chosenLoc == -1) {
            System.out.println("No available location found for gcrMuzzleLightContributions");
            IrisExtendRT.updateAttachmentLayout(-1);
            return fsh;
        }
        IrisExtendRT.updateAttachmentLayout(chosenLoc);

        String fshOut = fsh;

        // 1. 在 #version 之后插入需要的 uniform 声明（如果这份 fsh 本身还没有）
        fshOut = insertMissingUniformsAfterVersion(fshOut);

        // 2. 插入 layout(out) 输出声明 + in 声明（放在第一个 out 变量之前，找不到就放在 main 之前）
        String layoutDecl = "layout(location = " + chosenLoc + ") out vec2 gcrMuzzleLightContributions;";
        String inDecl = "in float gcrMuzzleLightContribution;\nin vec2 gcrUV0;";
        fshOut = insertDeclarationsBeforeFirstOutOrMain(fshOut, layoutDecl + "\n" + inDecl);

        // 3. 在 main() 函数体第一行插入赋值代码
        String code =
                """
                float gcrHeatRes = gcrHeat * texture(gcrHeatMap, gcrUV0).r;
                gcrMuzzleLightContributions = vec2(gcrMuzzleLightContribution, gcrHeatRes);
                """;
        fshOut = insertAtStartOfMain(fshOut, code);

        return fshOut;
    }

    /**
     * 从已经 patch 过的 fsh 中提取 gcrMuzzleLightContributions 的 location 值。
     * 逐行扫描，找到包含 "out vec2 gcrMuzzleLightContributions" 的那一行，
     * 再从 "location = " 和 ")" 之间截取数字。
     */
    private static int extractExistingLocation(String fsh) {
        for (String line : fsh.split("\n")) {
            if (!line.contains("gcrMuzzleLightContributions") || !line.contains("layout")) {
                continue;
            }
            int locKeywordPos = line.indexOf("location");
            if (locKeywordPos == -1) {
                continue;
            }
            int eqPos = line.indexOf('=', locKeywordPos);
            int closeParenPos = line.indexOf(')', eqPos);
            if (eqPos == -1 || closeParenPos == -1) {
                continue;
            }
            String numberPart = line.substring(eqPos + 1, closeParenPos).trim();
            try {
                return Integer.parseInt(numberPart);
            } catch (NumberFormatException ignored) {
                // 格式不标准，继续找下一行（理论上不应该发生）
            }
        }
        return -1;
    }

    /**
     * 逐行扫描 fsh，收集所有已经被占用的 "layout(location = X) out ..." 的 X 值，
     * 然后从 0 开始找第一个没被占用的 location。
     */
    private static int findFreeAttachmentLocation(String fsh, int maxMRT) {
        java.util.Set<Integer> usedLocations = new java.util.HashSet<>();

        for (String line : fsh.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("layout") || !trimmed.contains("out ")) {
                continue;
            }
            int locKeywordPos = trimmed.indexOf("location");
            if (locKeywordPos == -1) {
                continue;
            }
            int eqPos = trimmed.indexOf('=', locKeywordPos);
            int closeParenPos = trimmed.indexOf(')', eqPos);
            if (eqPos == -1 || closeParenPos == -1) {
                continue;
            }
            String numberPart = trimmed.substring(eqPos + 1, closeParenPos).trim();
            try {
                usedLocations.add(Integer.parseInt(numberPart));
            } catch (NumberFormatException ignored) {
                // 忽略格式不标准的行
            }
        }

        for (int i = 0; i < maxMRT; i++) {
            if (!usedLocations.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在 #version 那一行之后插入还缺失的 uniform 声明（HeatMap 采样器 + Heat 强度值）。
     * 跟 injectUniformsAndTransformLogicFP 里插入 vsh uniform 的做法完全一致。
     */
    private static String insertMissingUniformsAfterVersion(String fsh) {
        StringBuilder extraUniforms = new StringBuilder();
        if (!fsh.contains("uniform sampler2D gcrHeatMap;")) {
            extraUniforms.append("uniform sampler2D gcrHeatMap;\n");
        }
        if (!fsh.contains("uniform float gcrHeat;")) {
            extraUniforms.append("uniform float gcrHeat;\n");
        }
        if (extraUniforms.length() == 0) {
            return fsh;
        }

        int versionIndex = fsh.indexOf("#version");
        if (versionIndex == -1) {
            System.err.println("Shader is missing #version directive!");
            return fsh;
        }
        int extensionIndex = fsh.indexOf("#extension");
        int pragmaIndex = fsh.indexOf("#pragma");
        extensionIndex = Math.max(pragmaIndex, extensionIndex);
        versionIndex = Math.max(extensionIndex, versionIndex);

        int versionLineEnd = fsh.indexOf('\n', versionIndex) + 1;

        return fsh.substring(0, versionLineEnd) + extraUniforms + fsh.substring(versionLineEnd);
    }

    /**
     * 把声明文本插入到第一个 "out " 变量声明所在行之前；
     * 如果这份 fsh 里完全没有 out 变量，就插到 "void main" 之前；
     * 都找不到就插到文件开头（兜底）。
     */
    private static String insertDeclarationsBeforeFirstOutOrMain(String fsh, String declarations) {
        StringBuilder sb = new StringBuilder(fsh);

        int insertPos = sb.indexOf("out ");
        if (insertPos == -1) {
            insertPos = sb.indexOf("void main");
        }

        if (insertPos == -1) {
            sb.insert(0, declarations + "\n");
            return sb.toString();
        }

        // 回退到这一行的行首
        while (insertPos > 0 && sb.charAt(insertPos - 1) != '\n') {
            insertPos--;
        }
        sb.insert(insertPos, declarations + "\n");
        return sb.toString();
    }

    /**
     * 把代码插入到 main() 函数体的第一行（跳过左花括号后的空白）。
     */
    private static String insertAtStartOfMain(String fsh, String code) {
        int mainPos = fsh.indexOf("void main");
        if (mainPos == -1) {
            return fsh;
        }
        int bracePos = fsh.indexOf("{", mainPos);
        if (bracePos == -1) {
            return fsh;
        }

        int insertCodePos = bracePos + 1;
        while (insertCodePos < fsh.length() && Character.isWhitespace(fsh.charAt(insertCodePos))) {
            insertCodePos++;
        }

        return fsh.substring(0, insertCodePos) + code + "\n" + fsh.substring(insertCodePos);
    }

    private static String injectUniformsAndTransformLogicFP(String vsh) {
        if (vsh.contains("uniform int gcrDoTransformOverride;")) {
            return vsh;
        }

        int versionIndex = vsh.indexOf("#version");
        if (versionIndex == -1) {
            System.err.println("Shader is missing #version directive!");
            return vsh;
        }
        int extensionIndex = vsh.indexOf("#extension");
        int pragmaIndex = vsh.indexOf("#pragma");
        extensionIndex = Math.max(pragmaIndex, extensionIndex);
        versionIndex = Math.max(extensionIndex, versionIndex);
        String uniforms = "";
        if (!vsh.contains("uniform int gcrDoTransformOverride;")) {
            uniforms += "uniform int gcrDoTransformOverride;\n";
        }

        if (!vsh.contains("uniform vec3 MuzzleFlashPosition;")) {
            uniforms += "uniform vec3 MuzzleFlashPosition;\n";
        }

        if (!vsh.contains("uniform float MuzzleFlashIntensity;")) {
            uniforms += "uniform float MuzzleFlashIntensity;\n";
        }

        if (!vsh.contains("uniform float MuzzleFlashRadius;")) {
            uniforms += "uniform float MuzzleFlashRadius;\n";
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

        if (!vsh.contains("out float gcrMuzzleLightContribution;")) {
            uniforms += "out float gcrMuzzleLightContribution;\n";
        }

        if (!vsh.contains("out vec2 gcrUV0;")) {
            uniforms += "out vec2 gcrUV0;\n";
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
                gcrUV0 = iris_UV0;
                float gcrMuzzleLightContributionRes = 0.0;
                if (gcrDoTransformOverride == 1) {
                    int boneId = iris_UV1.x;
                    GcrBoneData data = gcrBones[boneId];
                
                    bool visible = data.PacketNormalLightVisible[3][2] > 0.5;
                
                    gcrTransformedNormal = mat3(data.PacketNormalLightVisible) * iris_Normal;
                    gcrTransformedNormal = normalize(gcrTransformedNormal);
                    gcrTransformedPos = data.TransMat * gcrTransformedPos;
                    
                    if (!visible) {
                         gcrTransformedPos.xyz = vec3(1e20);
                    }
                
                    gcrMixedLightmap = ivec2(data.PacketNormalLightVisible[3][0], data.PacketNormalLightVisible[3][1]);
                    gcrOverrideOverlay = ivec2(0, 10);

                    if (MuzzleFlashIntensity > 0.0) {
                        vec3 lightVector = MuzzleFlashPosition - gcrTransformedPos.xyz;
                        float distance = length(lightVector);
                        vec3 lightDir = normalize(lightVector);
                        float attenuation = 1.0 - smoothstep(0.0, MuzzleFlashRadius, distance);
                        attenuation *= MuzzleFlashIntensity;
                        float diffuse = dot(gcrTransformedNormal, lightDir);
                        diffuse = max(0.0, diffuse);
                        gcrMuzzleLightContributionRes = diffuse * attenuation;
                    }
                }
                gcrMuzzleLightContribution = gcrMuzzleLightContributionRes;
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


    private static String injectUniformsAndTransformLogic(String vsh) {

        if (vsh.contains("uniform int gcrDoTransformOverride;")) {
            return vsh;
        }

        int versionIndex = vsh.indexOf("#version");
        if (versionIndex == -1) {
            System.err.println("Shader is missing #version directive!");
            return vsh;
        }
        int extensionIndex = vsh.indexOf("#extension");
        int pragmaIndex = vsh.indexOf("#pragma");
        extensionIndex = Math.max(pragmaIndex, extensionIndex);
        versionIndex = Math.max(extensionIndex, versionIndex);
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

                
                    gcrTransformedNormal = mat3(data.PacketNormalLightVisible) * iris_Normal;
                    gcrTransformedNormal = normalize(gcrTransformedNormal);
                    gcrTransformedPos = data.TransMat * gcrTransformedPos;
                    
                    if (!visible) {
                         gcrTransformedPos.xyz = vec3(1e20);
                    }
                
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
