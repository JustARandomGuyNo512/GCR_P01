#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 v_ViewNormal;
out vec3 v_ViewPos;

void main() {
    vec4 viewPos4 = ModelViewMat * vec4(Position, 1.0);
    v_ViewPos = viewPos4.xyz;
    v_ViewNormal = normalize(mat3(ModelViewMat) * Position);
    gl_Position = ProjMat * viewPos4;
}