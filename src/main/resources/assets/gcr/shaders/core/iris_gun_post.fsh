#version 150

uniform sampler2D MuzzleLightContributionSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(MuzzleLightContributionSampler, texCoord);
    float light = color.r;
    float heat  = color.g;
    vec3 muzzleColor = vec3(1.0, 0.8, 0.5) * light;
    //shader pack下再偏白一点，可能是后处理挡掉了一部分吧。。。。
    float heat2 = heat * heat;
    vec3 heatColor = vec3(heat, heat2 * 0.7, heat2 * 0.45);
    vec3 finalColor = muzzleColor + heatColor;
    fragColor = vec4(finalColor, 1.0);
}