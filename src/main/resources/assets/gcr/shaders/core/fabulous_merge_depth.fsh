#version 150

uniform sampler2D MainDepth;
uniform sampler2D TranslucentDepth;
uniform sampler2D ItemDepth;
uniform sampler2D ParticlesDepth;
uniform sampler2D CloudsDepth;
uniform sampler2D WeatherDepth;

in vec2 texCoord;
out vec4 fragColor;

/**
 * 将一个 0.0-1.0 的浮点数打包到 vec3 (RGB) 中，
 * 提供 24 位精度。
 */
vec3 packDepth(float depth) {
    vec3 enc;
    enc.x = fract(depth * 1.0);
    enc.y = fract(depth * 255.0);
    enc.z = fract(depth * 65025.0);

    enc.x -= enc.y * (1.0 / 255.0);
    enc.y -= enc.z * (1.0 / 255.0);
    return enc;
}

void main() {
    float d0 = texture(MainDepth, texCoord).r;
    float d1 = texture(TranslucentDepth, texCoord).r;
    float d2 = texture(ItemDepth, texCoord).r;
    float d3 = texture(ParticlesDepth, texCoord).r;
    float d4 = texture(CloudsDepth, texCoord).r;
    float d5 = texture(WeatherDepth, texCoord).r;

    float depthMin = min(min(min(min(min(d0,d1),d2),d3),d4),d5);

    fragColor = vec4(packDepth(depthMin), 1.0);
}
