package me.buhlmann.engine4.core.gfx.vulkan.material;

import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;

public class VulkanTextureSC extends VulkanTexture
{
    public VulkanTextureSC(final int frame, final VulkanRenderContext vkContext)
    {
        super.vkImageView = vkContext.getSwapChain().getImageViews()[frame];
        super.vkTextureSampler = new VulkanTextureSampler(vkContext, 1, false);
        super.vkImage = null;
    }

    @Override
    public void dispose()
    {
        super.vkImageView.dispose();
    }
}
