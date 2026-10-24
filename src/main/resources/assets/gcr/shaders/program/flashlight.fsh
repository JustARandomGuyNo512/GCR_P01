#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform mat4 ProjMat;
uniform vec2 OutSize;
uniform vec2 TexelSize;

uniform mat4 InversePVMat;
uniform vec3 To;
uniform float Angle;
uniform float Range;
uniform float Luminance;
uniform float MinZ;
uniform int Mode;

in vec2 texCoord;
out vec4 fragColor;

vec3 getFragWorldPos(vec2 coord) {
    float depth = texture(DiffuseDepthSampler, coord).r;
    vec4 clipPos = vec4(coord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 worldPos = InversePVMat * clipPos;
    return worldPos.xyz / worldPos.w;
}


void main(){
    vec4 diffuseColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;

    vec3 pos = getFragWorldPos(texCoord);
    float dist = length(pos);

    if (dist > Range || depth < MinZ) {
        fragColor = vec4(diffuseColor.rgb, 1.0);
        return;
    }

    float angleCos = dot(normalize(pos), To);
    float disToCenter = sqrt(1.0 - angleCos * angleCos);

    float falloff = (Range - dist) / Range;
    float intensity = clamp(exp(-disToCenter * 15.0) * Luminance / (dist * 0.015) * falloff, 0.0, 2.5);

    float luma = dot(diffuseColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    float dynamicIntensity = intensity * (1.0 - luma);

    vec3 lightBase = pow(diffuseColor.rgb, vec3(0.68));
    vec3 addLight = lightBase * dynamicIntensity;

    vec3 adjustedColor = vec3(1.0) - (vec3(1.0) - diffuseColor.rgb) * (vec3(1.0) - clamp(addLight, 0.0, 1.0));

    fragColor = vec4(adjustedColor, 1.0);
}