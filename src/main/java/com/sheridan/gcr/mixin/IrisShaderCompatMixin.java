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
        // 如果已经包含 gcrMuzzleLightContributions，则提取已有的 location 并赋值给 MuzzleRT.attachmentLayoutLocation
        if (fsh.contains("gcrMuzzleLightContributions")) {
            // 匹配 layout(location = X) out float gcrMuzzleLightContributions;
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*out\\s+float\\s+gcrMuzzleLightContributions\\s*;");
            java.util.regex.Matcher matcher = pattern.matcher(fsh);
            if (matcher.find()) {
                int loc = Integer.parseInt(matcher.group(1));
                IrisExtendRT.updateAttachmentLayout(loc);
            } else {
                // 虽然包含名称，但 layout 格式不标准，也设为 -1
                IrisExtendRT.updateAttachmentLayout(-1);
            }
            return fsh; // 不做修改，直接返回
        }

        int maxDrawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        int maxColorAttachments = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
        int maxMRT = Math.min(maxDrawBuffers, maxColorAttachments); // 默认保守值，可根据设备查询调整

        // 收集 fsh 中所有已使用的 layout(location = X) 的 X 值
        java.util.Set<Integer> usedLocations = new java.util.HashSet<>();
        java.util.regex.Pattern locPattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*out\\s+\\w+");
        java.util.regex.Matcher locMatcher = locPattern.matcher(fsh);
        while (locMatcher.find()) {
            try {
                int loc = Integer.parseInt(locMatcher.group(1));
                usedLocations.add(loc);
            } catch (NumberFormatException ignored) {
            }
        }

        // 寻找最小的非负空闲 location（从 0 开始，连续查找空位）
        int chosenLoc = -1;
        for (int i = 0; i < maxMRT; i++) {
            if (!usedLocations.contains(i)) {
                chosenLoc = i;
                break;
            }
        }

        // 如果没有可用空位，设为 -1 并直接返回原 shader
        if (chosenLoc == -1) {
            System.out.println("No available location found for gcrMuzzleLightContributions");
            IrisExtendRT.updateAttachmentLayout(-1);
            return fsh;
        }

        // 有可用位置，赋值
        IrisExtendRT.updateAttachmentLayout(chosenLoc);

        // 准备要注入的字符串
        String layout = "layout(location = " + chosenLoc + ") out float gcrMuzzleLightContributions;";
        String inDecl = "in float gcrMuzzleLightContribution;";  // 注意：原文中是 in float gcrMuzzleLightContribution（无 s）
        String code = "gcrMuzzleLightContributions = gcrMuzzleLightContribution;";

        // 1. 将 layout 声明注入到所有 out 变量声明之后（或文件开头合适位置，通常在 precision/uniform 之后）
        //    这里简单地在第一个 out 出现的位置之前插入，或文件开头后合适处
        //    更健壮的做法：找到最后一个 layout(out) 或 uniform 之后插入
        StringBuilder sb = new StringBuilder(fsh);

        // 查找合适插入 layout 的位置：通常在 #version 或 precision 之后，所有 in/out/uniform 声明区域
        // 这里采用一个简单策略：在第一个 "out " 出现之前插入（如果有 out），否则在文件开头合理位置
        int insertLayoutPos = sb.indexOf("out ");
        if (insertLayoutPos != -1) {
            // 找到该 out 行的开头（向上找换行）
            while (insertLayoutPos > 0 && sb.charAt(insertLayoutPos - 1) != '\n') {
                insertLayoutPos--;
            }
            sb.insert(insertLayoutPos, layout + "\n");
        } else {
            // 没有 out，插入到文件较前面（例如第一个 { 或 main 之前）
            insertLayoutPos = sb.indexOf("void main");
            if (insertLayoutPos != -1) {
                while (insertLayoutPos > 0 && sb.charAt(insertLayoutPos - 1) != '\n') {
                    insertLayoutPos--;
                }
                sb.insert(insertLayoutPos, layout + "\n");
            } else {
                // 兜底：头部添加
                sb.insert(0, layout + "\n");
            }
        }

        // 2. 将 in 声明注入（通常在 layout 附近，或所有 in 声明区域）
        //    这里复用类似逻辑，插入在 layout 之后
        int inInsertPos = sb.indexOf(layout);
        if (inInsertPos != -1) {
            inInsertPos += layout.length() + 1; // 换行后
            sb.insert(inInsertPos, inDecl + "\n");
        } else {
            // 兜底
            sb.insert(0, inDecl + "\n");
        }

        // 3. 将 code 注入到 void main() 的第一行内部
        int mainPos = sb.indexOf("void main");
        if (mainPos != -1) {
            // 找到 main 的 {
            int bracePos = sb.indexOf("{", mainPos);
            if (bracePos != -1) {
                // 跳过 { 和可能的空白，插入在第一行
                int insertCodePos = bracePos + 1;
                while (insertCodePos < sb.length() &&
                        Character.isWhitespace(sb.charAt(insertCodePos))) {
                    insertCodePos++;
                }
                sb.insert(insertCodePos, code + "\n");
            }
        }

        return sb.toString();
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
                float gcrMuzzleLightContributionRes = 0.0;
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
