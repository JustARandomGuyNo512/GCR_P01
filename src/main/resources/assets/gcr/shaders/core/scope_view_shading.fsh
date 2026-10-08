#version 150

uniform sampler2D uSceneColor;
uniform vec2  uResolution;
uniform vec2  uLensCenter;
uniform float uLensRadius;

uniform vec2  uEyeOffset;
uniform float uEyeDistance;
uniform float uSensitivity;

uniform float uInnerFade;
uniform float uOuterFade;
uniform float uVignettePower;
uniform float uDistortionStrength;
uniform float uDistortionFalloff;
uniform vec3 uGlassColor;
uniform float uGlassMix;
uniform float uTexMagnification;

in vec2 texCoord;
out vec4 FragColor;

void main() {
    vec2 parallaxPx = uEyeOffset * uEyeDistance * uSensitivity * uLensRadius;
    vec2 lensCenter = uLensCenter + parallaxPx;
    vec2 p = texCoord * uResolution - lensCenter;
    float rNorm = length(p) / max(uLensRadius, 1.0);

    float radial = pow(clamp(rNorm, 0.0, 1.0), uDistortionFalloff);
    vec2 samplePx = lensCenter + p * (1.0 + uDistortionStrength * radial) / uTexMagnification;
    vec3 scene = texture(uSceneColor, clamp(samplePx / uResolution, vec2(0.0), vec2(1.0))).rgb;
    vec3 glass = mix(scene, scene * uGlassColor, uGlassMix);

    float rim = smoothstep(uInnerFade, uOuterFade, rNorm);
    float vignette = pow(clamp(rNorm, 0.0, 1.0), uVignettePower);
    vec3 color = mix(glass * (1.0 - 0.18 * vignette), vec3(0.0), rim);
    FragColor = vec4(color, 1.0);
}
