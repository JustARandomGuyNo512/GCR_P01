#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D depth;
uniform sampler2D gcrHeatMap;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
//0~1
uniform float gcrHeat;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 muzzleLightContribution;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.05) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color *= lightMapColor;
    color.rgb += muzzleLightContribution;

    float heat = gcrHeat * texture(gcrHeatMap, texCoord0).r;
    float heat2 = heat * heat;
    vec3 heatColor = vec3(
    heat,
    heat2 * 0.5,
    heat2 * 0.3);

    color.rgb += heatColor;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}