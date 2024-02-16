package me.buhlmann.engine4.core.gfx.vulkan.material;

import me.buhlmann.engine4.api.IDisposable;
import me.buhlmann.engine4.api.gfx.texture.ITexture;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanImage;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanImageView;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.AbstractVulkanAsset;

public abstract class VulkanTexture extends AbstractVulkanAsset implements ITexture, IVulkanDisposable, IDisposable
{
    protected VulkanImage vkImage;
    protected VulkanImageView vkImageView;
    protected VulkanTextureSampler vkTextureSampler; // Concrete part of VulkanTexture class for now.

    protected VulkanTexture()
    {
    }

    @Override
    public void dispose()
    {
        this.vkImageView.dispose();
        this.vkImage.dispose();
    }

    public VulkanImageView getImageView()
    {
        return this.vkImageView;
    }

    public VulkanTextureSampler getSampler()
    {
        return this.vkTextureSampler;
    }
}
