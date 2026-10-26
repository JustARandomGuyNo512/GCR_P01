#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
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
in vec3 gcrViewNormal;
in vec3 gcrViewPosition;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.05) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color *= lightMapColor;
    color.rgb += muzzleLightContribution;

    // Camera-space studio-style key/fill for the GCR vanilla shader only.
    // The previous 8.5% specular multiplied by the already-dark albedo was
    // almost invisible. Controlled broad diffuse relief makes real polygon
    // faces readable; the small white highlight gives black metal definition.
    // The Oculus path has its own bounded mesh vertex-lighting fallback.
    vec3 N = normalize(gcrViewNormal);
    vec3 V = normalize(-gcrViewPosition);
    vec3 keyDir = normalize(vec3(0.38, 0.76, 0.51));
    vec3 fillDir = normalize(vec3(-0.78, 0.37, 0.50));
    float keyDiffuse = max(0.0, dot(N, keyDir));
    float fillDiffuse = max(0.0, dot(N, fillDir));
    float faceRelief = 0.80 + 0.28 * keyDiffuse + 0.12 * fillDiffuse;
    color.rgb *= faceRelief;
    vec3 halfVector = normalize(keyDir + V);
    float specular = pow(max(dot(N, halfVector), 0.0), 28.0) * keyDiffuse;
    float fresnel = pow(1.0 - clamp(dot(N, V), 0.0, 1.0), 4.0);
    color.rgb += vec3(0.050) * specular + color.rgb * (0.035 * fresnel);

    float heat = gcrHeat * texture(gcrHeatMap, texCoord0).r;
    float heat2 = heat * heat;
    vec3 heatColor = vec3(
    heat,
    heat2 * 0.5,
    heat2 * 0.3);

    color.rgb += heatColor;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}