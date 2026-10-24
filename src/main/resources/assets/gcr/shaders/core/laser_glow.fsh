#version 150

in vec3 v_ViewNormal;
in vec3 v_ViewPos;
out vec4 FragColor;

uniform vec4 GlowColor;

void main() {
    vec3 normal = normalize(v_ViewNormal);
    vec3 viewDir = normalize(-v_ViewPos);
    float centerFactor = max(dot(normal, viewDir), 0.0);
    float coreGlow = pow(centerFactor, 2.0);
    FragColor = vec4(GlowColor.rgb, GlowColor.a * coreGlow);
}