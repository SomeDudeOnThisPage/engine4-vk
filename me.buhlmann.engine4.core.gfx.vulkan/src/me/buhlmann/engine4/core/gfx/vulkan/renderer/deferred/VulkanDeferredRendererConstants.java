package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.api.gfx.ShaderAttribute;
import me.buhlmann.engine4.api.gfx.primitive.VertexBufferLayout;

public final class VulkanDeferredRendererConstants
{
    public static VertexBufferLayout VK_GEOMETRY_MODEL_PIPELINE_LAYOUT = new VertexBufferLayout(
        new VertexBufferLayout.BufferElement(ShaderAttribute.V_POSITION),
        new VertexBufferLayout.BufferElement(ShaderAttribute.V_NORMAL),
        new VertexBufferLayout.BufferElement(ShaderAttribute.V_TEXTURE)
    );

    private VulkanDeferredRendererConstants()
    {

    }
}
