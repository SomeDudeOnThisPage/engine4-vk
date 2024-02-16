package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;

public interface IVulkanRenderer extends IRenderer
{
    VulkanRenderContext getRenderContext();
}
