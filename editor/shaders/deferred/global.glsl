#ifndef PIPELINE_DEFERRED_GLOBAL_H
#define PIPELINE_DEFERRED_GLOBAL_H

    _E4C_PIPELINE
    {
        vec4 camera_position; // UBO offset generation testing...
        mat4 view;
        mat4 projection;
    } u_pipeline;

    #ifdef FRAGMENT_SHADER
        // TODO: Make this dependent on injected constants?
        layout(location = 0) out vec4 _gb_position;
        layout(location = 1) out vec4 _gb_albedo;
        layout(location = 2) out vec4 _gb_normal;

        // TODO: allow arbitrary buffer data
        // void _set_buffer(int gb_buffer, vec4 data)
        // {
        // }

        // Writes values to the gbuffer's position texture data slot.
        void set_fragment_position(vec3 position)
        {
            _gb_position = vec4(position, 1.0f);
        }

        // Writes values to the gbuffer's albedo texture data slot.
        void set_albedo(vec3 albedo)
        {
            _gb_albedo = vec4(albedo, 1.0f);
        }

        // Writes values to the gbuffer's normal texture data slot.
        void set_normal(vec3 normal)
        {
            _gb_normal = vec4(normal, 1.0f);
        }
    #endif // FRAGMENT_SHADER

#endif // PIPELINE_DEFERRED_GLOBAL_H