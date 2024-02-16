#ifdef LIGHTING_SHADER
#include <light.glsl>

#ifdef POINT_LIGHT
    void light(const PointLight_t point_light)
    {

    }
#endif // POINT_LIGHT

#ifdef DIRECTIONAL_LIGHT
    void light(const DirectionalLight_t point_light)
    {

    }
#endif // DIRECTIONAL_LIGHT

// #ifdef CONE_LIGHT
//     void light()
//     {
//
//     }
// #endif // CONE_LIGHT

#endif // LIGHTING_SHADER
