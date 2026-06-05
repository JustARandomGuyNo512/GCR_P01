#version 150

uniform sampler2D DepthSampler;
in vec2 texCoord;
void main() {
    float depth = texture(DepthSampler, texCoord).r;
    gl_FragDepth = depth;
}