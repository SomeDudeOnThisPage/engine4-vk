package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.texture.ITexture;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTexture;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTexture2D;
import org.lwjgl.vulkan.VK11;

public class VulkanUniformSampler extends VulkanDescriptor
{
    private final VulkanDescriptorSet.DescriptorBinding binding;

    public VulkanDescriptorSet.DescriptorBinding getBinding()
    {
        return this.binding;
    }

    public VulkanUniformSampler(int binding, final IAssetReference<ITexture> sampler)
    {
        if (sampler.get() instanceof VulkanTexture vkTexture)
        {
            this.binding = new VulkanDescriptorSet.DescriptorBinding(
                VK11.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK11.VK_SHADER_STAGE_ALL_GRAPHICS,
                binding,
                vkTexture
            );
        }
        else
        {
            throw new IllegalStateException("wrong internal Texture2D type");
        }
    }

    public VulkanUniformSampler(final IMaterial.Sampler sampler)
    {
        this(sampler.binding(), sampler.sampler());
    }
}
