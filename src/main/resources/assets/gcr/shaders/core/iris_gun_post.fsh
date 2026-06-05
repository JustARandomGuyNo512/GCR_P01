#version 150

uniform sampler2D MuzzleLightContributionSampler;
in vec2 texCoord;

out vec4 fragColor;

void main() {
    float light = texture(MuzzleLightContributionSampler, texCoord).r;

    fragColor = vec4(1.0, 0.8, 0.5, light);
}