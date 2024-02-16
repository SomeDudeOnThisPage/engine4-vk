#version 450
#include <light.glsl>

struct PointLight
{
    vec4 pos;
    vec4 col;
    vec4 clq;
};

layout(location = 0) in vec2 f_uv;
layout(location = 0) out vec4 f_out;

layout(set = 0, binding = 0) uniform u_point_light_buffer
{
    PointLight light[1];
    uint amount;
} u_point_lights;

// TODO: GBuffer should logically be its' own set!
layout(set = 0, binding = 1) uniform sampler2D gb_position;
layout(set = 0, binding = 2) uniform sampler2D gb_albedo;
layout(set = 0, binding = 3) uniform sampler2D gb_normal;

const vec3 LIGHT_POSITION = vec3(0.0f, 4.0f, 0.0f);

void main()
{
    float diff = 0.0f;
    for (uint i = 0; i < u_point_lights.amount; i++)
    {
        PointLight light = u_point_lights.light[i];
        vec3 normal = normalize(texture(gb_normal, f_uv).rgb);
        vec3 light_dir = normalize(light.pos.xyz - texture(gb_position, f_uv).rgb);

        diff = diff + max(dot(normal, light_dir), 0.0);
    }

    f_out = vec4(min(diff + 0.001, 1.0) * texture(gb_albedo, f_uv).rgb, 1.0f);
}
