#ifdef POINT_LIGHT
    #define MAX_POINT_LIGHTS 24

    struct PointLight_t
    {
        vec4 pos;
        vec4 col;
        vec4 clq;
    };

    layout(set = 0, binding = 0) uniform u_point_light_buffer
    {
        PointLight_t light[MAX_POINT_LIGHTS];
        uint amount;
    } u_point_lights;

    void main()
    {
        light(u_point_lights);
    }
#endif // POINT_LIGHT

#ifdef DIRECTIONAL_LIGHT
    #define MAX_DIRECTIONAL_LIGHTS 8

    struct DirectionalLight_t
    {
        vec4 col;
    };

    layout(set = 1, binding = 0) uniform u_directional_light_buffer
    {
        DirectionalLight_t light[MAX_DIRECTIONAL_LIGHTS];
        uint amount;
    } u_directional_lights;
#endif // POINT_LIGHT