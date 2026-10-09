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
uniform vec3 uDistortion;
uniform vec3 uGlassColor;
uniform float uGlassMix;
uniform float uTexMagnification;
uniform float uTimeSinceShot;
uniform float uFlashSensitivity;
uniform float uFlashYOffset;

in vec2 texCoord;
out vec4 FragColor;

void main() {
    vec2 parallaxPx = uEyeOffset * uEyeDistance * uSensitivity * uLensRadius;
    vec2 lensCenter = uLensCenter + parallaxPx;
    vec2 p = texCoord * uResolution - lensCenter;
    float rNorm = length(p) / max(uLensRadius, 1.0);

    float r = clamp(rNorm, 0.0, 1.0);
    float radial = pow(r, max(uDistortion.y * uDistortion.z, 0.001));
    vec2 samplePx = lensCenter + p * (1.0 + uDistortion.x * radial) / uTexMagnification;
    vec3 scene = texture(uSceneColor, clamp(samplePx / uResolution, vec2(0.0), vec2(1.0))).rgb;
    vec3 glass = mix(scene, scene * uGlassColor, uGlassMix);

    float rim = smoothstep(uInnerFade, uOuterFade, rNorm);
    float vignette = pow(clamp(rNorm, 0.0, 1.0), uVignettePower);
    vec3 color = glass * (1.0 - 0.18 * vignette);

    if (uTimeSinceShot >= 0.0 && uTimeSinceShot < 0.12 && uFlashSensitivity > 0.0) {
        float pulse = pow(max(1.0 - uTimeSinceShot / 0.04, 0.0), 1.5);
        float afterglow = 0.12 * exp(-uTimeSinceShot / 0.035)
                * (1.0 - smoothstep(0.06, 0.12, uTimeSinceShot));
        float flash = (pulse + afterglow) * uFlashSensitivity;
        vec2 lensUV = p / max(uLensRadius, 1.0);
        vec2 lightCenter = vec2(0.0, uFlashYOffset) - parallaxPx / max(uLensRadius, 1.0) * 0.35;
        vec2 lightDelta = (lensUV - lightCenter) * vec2(0.85, 1.25);
        float scatter = exp(-dot(lightDelta, lightDelta) * 2.8);
        float halo = exp(-pow((length(lensUV + lightCenter * 0.45) - 0.62) / 0.11, 2.0));
        float tubeReflection = exp(-pow((rNorm - 0.87) / 0.09, 2.0));
        float lowerRim = 0.35 + 0.65 * (1.0 - smoothstep(-0.8, 0.5, lensUV.y));
        vec3 flashColor = vec3(1.0, 0.63, 0.26);

        // Linear light lets exposure and optical scatter respond to the scene luminance.
        vec3 linearColor = pow(max(color, vec3(0.0)), vec3(2.2));
        linearColor *= 1.0 + flash * (0.3 + 0.7 * scatter);
        vec3 opticalScatter = flashColor * (0.32 * scatter + 0.13 * tubeReflection * lowerRim)
                + vec3(0.42, 0.62, 1.0) * halo * 0.055;
        // Exponential transmittance bounds the scattered light without a flat color overlay.
        linearColor = vec3(1.0) - (vec3(1.0) - clamp(linearColor, 0.0, 1.0))
                * exp(-opticalScatter * flash);
        color = pow(max(linearColor, vec3(0.0)), vec3(1.0 / 2.2));
    }

    color *= 1.0 - rim;
    FragColor = vec4(color, 1.0);
}
