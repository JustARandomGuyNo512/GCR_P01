#version 150

uniform vec2  uResolution;
uniform vec2  uLensCenter;
uniform float uLensRadius;

uniform vec2  uEyeOffset;
uniform float uEyeDistance;
uniform float uSensitivity;

uniform float uInnerFade;
uniform float uOuterFade;
uniform float uVignettePower;

in vec2 texCoord;
out vec4 FragColor;

void main() {
    vec2 fragPx = texCoord * uResolution;
    vec2 p = fragPx - uLensCenter;
    vec2 parallaxPx = uEyeOffset * uEyeDistance * uSensitivity * uLensRadius;
    p -= parallaxPx;
    float r = length(p);
    float rNorm = r / uLensRadius;
    float mask = smoothstep(uInnerFade, uOuterFade, rNorm);
    float vignette = pow(clamp(rNorm, 0.0, 1.0), uVignettePower);
    float alpha = mask * vignette;
    alpha = clamp(alpha, 0.1, 1.0);
    FragColor = vec4(0.0, 0.0, 0.0, alpha);
}