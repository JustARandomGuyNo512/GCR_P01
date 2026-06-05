#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;//not used
in vec3 Normal;

struct GcrBoneData {
    mat4 TransMat;
    mat4 PacketNormalLightVisible;
};

layout (std140) uniform GcrBoneUBO {
    GcrBoneData bones[128];
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
    int boneId = UV1.x;
    GcrBoneData data = bones[boneId];


    bool visible = data.PacketNormalLightVisible[3][2] > 0.5;
    if (!visible) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }


    mat3 normalTrans = mat3(data.PacketNormalLightVisible);
    vec3 transformedNormal = normalTrans * Normal;
    transformedNormal = normalize(transformedNormal);
    vec4 transformedPos = data.TransMat * vec4(Position, 1.0);


    ivec2 light = ivec2(data.PacketNormalLightVisible[3][0], data.PacketNormalLightVisible[3][1]);
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