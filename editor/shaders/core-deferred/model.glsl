#ifndef PIPELINE_DEFERRED_MODEL_H
#define PIPELINE_DEFERRED_MODEL_H
    layout(push_constant) uniform push_constant_buffer
    {
        mat4 model;
    } pc_model;
#endif // PIPELINE_DEFERRED_MODEL_H
