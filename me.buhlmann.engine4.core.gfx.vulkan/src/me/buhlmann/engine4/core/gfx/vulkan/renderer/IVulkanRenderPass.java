package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.api.entity.IEntityCollection;
import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanDescriptorSetLayout;

public interface IVulkanRenderPass extends IVulkanDisposable
{
    Class<? extends IEntityCollection> getEntityCollectionType();

    void resize(final VulkanRenderContext vkContext);
    void render(final IRenderer.Input input, final VulkanRenderContext vkContext);

    long[] getDescriptorSetPointers();
    VulkanDescriptorSetLayout[] getDescriptorSetLayouts();
    long getPointer();

    void finish();
}
