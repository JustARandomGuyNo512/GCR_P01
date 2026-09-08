#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;//not used
in vec3 Normal;

layout (std140) uniform GcrBoneUBO {
    vec4 gcrBoneData[1024];
};

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

uniform vec3 MuzzleFlashPosition;
uniform float MuzzleFlashIntensity;
uniform float MuzzleFlashRadius;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec3 muzzleLightContribution;

void main() {
    int boneId = clamp(UV1.x, 0, 127);
    int base = boneId * 8;
    mat4 transMat = mat4(gcrBoneData[base], gcrBoneData[base + 1], gcrBoneData[base + 2], gcrBoneData[base + 3]);
    mat4 normalLightVisible = mat4(gcrBoneData[base + 4], gcrBoneData[base + 5], gcrBoneData[base + 6], gcrBoneData[base + 7]);


    bool visible = normalLightVisible[3][2] > 0.5;
    if (!visible) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }


    mat3 normalTrans = mat3(normalLightVisible);
    vec3 transformedNormal = normalTrans * Normal;
    transformedNormal = normalize(transformedNormal);
    vec4 transformedPos = transMat * vec4(Position, 1.0);


    ivec2 light = ivec2(normalLightVisible[3][0], normalLightVisible[3][1]);
    gl_Position = ProjMat * ModelViewMat * transformedPos;


    vec3 lightVector = MuzzleFlashPosition - transformedPos.xyz;
    float distance = length(lightVector);
    vec3 lightDir = normalize(lightVector);
    float attenuation = 1.0 - smoothstep(0.0, MuzzleFlashRadius, distance);
    attenuation *= MuzzleFlashIntensity;
    float diffuse = dot(transformedNormal, lightDir);
    diffuse = max(0.0, diffuse);
    muzzleLightContribution = vec3(1.0, 0.8, 0.5) * diffuse * attenuation;


    vertexDistance = fog_distance(Position, FogShape);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, transformedNormal, Color);
    lightMapColor = texelFetch(Sampler2, light / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
}
