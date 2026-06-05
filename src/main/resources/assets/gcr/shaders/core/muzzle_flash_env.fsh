#version 150

uniform sampler2D DiffuseDepthSampler;
uniform float flashIntensity;
uniform float minDepth;
uniform float maxDepth;
uniform float cameraNF;
uniform float cameraN_F;
uniform float cameraFNDist;
uniform float lightRadius;
uniform float aspectRatio;
uniform vec2 texelSize;
uniform bool isFabulousMode;

in vec2 texCoord;
out vec4 fragColor;

float unpackDepth(vec3 p)
{
    float d = p.x;
    d += p.y * 0.00392156862745;
    d += p.z * 0.0000152590218967;
    return d;
}

float getLinearEyeDepth(float d_nonlinear) {
    float z_ndc = 2.0 * d_nonlinear - 1.0;
    return cameraNF / (cameraN_F - z_ndc * cameraFNDist);
}

float getDepthAt(vec2 uv) {
    float d_nonlinear = 0.0;
    if (isFabulousMode) {
        vec3 packedDepth = texture(DiffuseDepthSampler, uv).rgb;
        d_nonlinear = unpackDepth(packedDepth);
    } else {
        d_nonlinear = texture(DiffuseDepthSampler, uv).r;
    }
    return d_nonlinear;
}

float getLinearDepthAt(vec2 uv) {
    float res = getDepthAt(uv);
    return getLinearEyeDepth(res);
}

vec3 getViewPos(vec2 uv) {
    float z = getLinearDepthAt(uv);
    vec2 ndc_xy = (uv - 0.5) * 2.0;
    ndc_xy.x *= aspectRatio;
    return z * vec3(ndc_xy, -1);
}

void main() {
    float currentDepth = getDepthAt(texCoord);
    if (currentDepth <= 0.0) {
        discard;
    }

    currentDepth = getLinearEyeDepth(currentDepth);
    if (currentDepth > maxDepth || currentDepth < minDepth) {
        discard;
    }

    vec3 v_pos = getViewPos(texCoord);
    vec3 normal = normalize(cross(dFdx(v_pos), dFdy(v_pos)));

    vec2 ndc_xy = (texCoord - 0.5) * 2.0;
    ndc_xy.x *= aspectRatio;
    float horizontalDistanceToCenter = length(ndc_xy * currentDepth);

    float depthAttenuation = 1.0 - smoothstep(minDepth, maxDepth, currentDepth);
    float radialAttenuation = 1.0 - smoothstep(0.0, lightRadius, horizontalDistanceToCenter);
    radialAttenuation *= radialAttenuation;

    float finalAttenuation = depthAttenuation * radialAttenuation;
    finalAttenuation = clamp(finalAttenuation, 0.0, 1.0);

    vec3 viewDir = normalize(-v_pos);

    float diffuse = dot(normal, viewDir) * 0.7 + 0.3;
    diffuse = max(diffuse, 0.0);

    float b = flashIntensity * finalAttenuation * diffuse;

    fragColor = vec4(1.0, 0.8, 0.5, b);
}