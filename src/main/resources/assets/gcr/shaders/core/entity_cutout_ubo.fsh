#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D depth;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

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
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}