#version 450 core

#ifdef SYNTAX_HIGHLIGHT_INSPECTION_FIX
    #define V_POSITION_ATTRIBUTE layout(location = 0) in vec3
    #define V_TEXTURE_ATTRIBUTE layout(location = 1) in vec2
    #define V_NORMAL_ATTRIBUTE layout(location = 1) in vec2
#endif

_E4_MATERIAL
{
    vec4 color;
} u_material;

_E4_MATERIAL_SAMPLER_1 u_albedo1;

#ifdef VERTEX_SHADER
    #include <deferred/global.glsl>
    #include <model.glsl>

    V_POSITION_ATTRIBUTE v_position;
    V_TEXTURE_ATTRIBUTE v_uv;
    V_NORMAL_ATTRIBUTE v_normal;

    layout(location = 0) out vec3 f_position;
    layout(location = 1) out vec2 f_uv;
    layout(location = 2) out vec3 f_normal;

    void main()
    {
        f_position = (pc_model.model * vec4(v_position, 1.0f)).xyz;
        f_uv = v_uv;
        f_normal = v_normal;

        gl_Position = u_pipeline.projection * u_pipeline.view * pc_model.model * vec4(v_position, 1.0f);
    }
#endif // VERTEX_SHADER

#ifdef FRAGMENT_SHADER
    #include <deferred/global.glsl>
    layout(location = 0) in vec3 f_position;
    layout(location = 1) in vec2 f_uv;
    layout(location = 2) in vec3 f_normal;

    void main()
    {
        set_fragment_position(f_position);
        set_albedo(texture(u_albedo1, f_uv).rgb);
        set_normal(f_normal);
    }
#endif // FRAGMENT_SHADER
